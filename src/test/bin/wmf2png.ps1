param(
	[Parameter(Mandatory = $true, Position = 0)]
	[string]$InputPath,

	[Parameter(Position = 1)]
	[string]$OutputPath,

	[int]$Width = 0,
	[int]$Height = 0,
	[int]$Dpi = 96,
	[switch]$Transparent
)

Set-StrictMode -Version 2.0
$ErrorActionPreference = "Stop"

function Convert-PathForWindows([string]$Path, [switch]$ForOutput) {
	if ([string]::IsNullOrWhiteSpace($Path)) {
		return $Path
	}

	if ($Path -match "^[A-Za-z]:\\" -or $Path -match "^\\\\") {
		return $Path
	}

	if (Test-Path -LiteralPath $Path) {
		return (Convert-Path -LiteralPath $Path)
	}

	if ($Path.StartsWith("/")) {
		$converted = (& wsl.exe wslpath -w -- "$Path" 2>$null)
		if ($LASTEXITCODE -eq 0 -and -not [string]::IsNullOrWhiteSpace($converted)) {
			return $converted.Trim()
		}
	}

	if ($ForOutput) {
		$parent = Split-Path -Parent $Path
		$name = Split-Path -Leaf $Path
		if (-not [string]::IsNullOrWhiteSpace($parent) -and (Test-Path -LiteralPath $parent)) {
			return (Join-Path (Convert-Path -LiteralPath $parent) $name)
		}
	}

	return $Path
}

function Read-UInt16LE([byte[]]$Bytes, [int]$Offset) {
	return [System.BitConverter]::ToUInt16($Bytes, $Offset)
}

function Read-Int16LE([byte[]]$Bytes, [int]$Offset) {
	return [System.BitConverter]::ToInt16($Bytes, $Offset)
}

function Read-UInt32LE([byte[]]$Bytes, [int]$Offset) {
	return [System.BitConverter]::ToUInt32($Bytes, $Offset)
}

function Test-WmfPlaceableHeader([string]$Path) {
	$bytes = [System.IO.File]::ReadAllBytes($Path)
	return $bytes.Length -ge 22 -and (Read-UInt32LE $bytes 0) -eq [uint32]2596720087
}

function Get-WmfWindowExtent([string]$Path) {
	$bytes = [System.IO.File]::ReadAllBytes($Path)
	$pos = 18
	$windowWidth = 0
	$windowHeight = 0
	while ($pos + 6 -le $bytes.Length) {
		$recordSizeWords = Read-UInt32LE $bytes $pos
		$recordSize = [int]($recordSizeWords * 2)
		if ($recordSize -lt 6 -or $pos + $recordSize -gt $bytes.Length) {
			break
		}

		$function = Read-UInt16LE $bytes ($pos + 4)
		if ($function -eq 0x020C -and $recordSize -ge 10) {
			$windowHeight = Read-Int16LE $bytes ($pos + 6)
			$windowWidth = Read-Int16LE $bytes ($pos + 8)
		}

		$pos += $recordSize
	}

	if ($windowWidth -eq 0 -or $windowHeight -eq 0) {
		return $null
	}

	return [pscustomobject]@{
		Width = [Math]::Abs($windowWidth)
		Height = [Math]::Abs($windowHeight)
	}
}

function Test-UseNonPlaceablePlayEnh([string]$Path, [System.Drawing.Image]$Image) {
	if (Test-WmfPlaceableHeader $Path) {
		return $false
	}

	$extent = Get-WmfWindowExtent $Path
	if ($extent -eq $null) {
		return $false
	}

	# These high-resolution non-placeable WMFs match MS Paint better via SetWinMetaFileBits/PlayEnhMetaFile.
	# Lower-resolution non-placeable files such as 123.wmf already match Paint through GDI+ DrawImage.
	return $Image.HorizontalResolution -gt 200 -and $Image.VerticalResolution -gt 200
}

function Get-WmfCanvasSize([string]$Path, [int]$GdiWidth, [int]$GdiHeight) {
	$bytes = [System.IO.File]::ReadAllBytes($Path)
	if ($bytes.Length -lt 18) {
		return $null
	}

	if ($bytes.Length -ge 22 -and (Read-UInt32LE $bytes 0) -eq [uint32]2596720087) {
		$left = Read-Int16LE $bytes 6
		$top = Read-Int16LE $bytes 8
		$right = Read-Int16LE $bytes 10
		$bottom = Read-Int16LE $bytes 12
		$inch = [Math]::Max(1, [int](Read-UInt16LE $bytes 14))
		$widthUnits = [Math]::Max(1, [int]$right - [int]$left)
		$heightUnits = [Math]::Max(1, [int]$bottom - [int]$top)
		$roundingBias = if ($left -ge 0 -and $top -ge 0) { 0.5 } else { 0.499999 }
		return [pscustomobject]@{
			Width = [Math]::Max(1, [int][Math]::Floor(($widthUnits * 144.0 / $inch) + $roundingBias))
			Height = [Math]::Max(1, [int][Math]::Floor(($heightUnits * 144.0 / $inch) + $roundingBias))
		}
	}

	$pos = 18
	$windowWidth = 0
	$windowHeight = 0
	while ($pos + 6 -le $bytes.Length) {
		$recordSizeWords = Read-UInt32LE $bytes $pos
		$recordSize = [int]($recordSizeWords * 2)
		if ($recordSize -lt 6 -or $pos + $recordSize -gt $bytes.Length) {
			break
		}

		$function = Read-UInt16LE $bytes ($pos + 4)
		if ($function -eq 0x020C -and $recordSize -ge 10) {
			$windowHeight = Read-Int16LE $bytes ($pos + 6)
			$windowWidth = Read-Int16LE $bytes ($pos + 8)
		}

		$pos += $recordSize
	}

	$width = [Math]::Abs($windowWidth)
	$height = [Math]::Abs($windowHeight)
	if ($width -gt 0 -and $height -gt 0) {
		if ($GdiWidth -ge $width -and $GdiWidth -le $width + 1 -and
				$GdiHeight -ge $height -and $GdiHeight -le $height + 1) {
			$width = $GdiWidth
			$height = $GdiHeight
		}
		return [pscustomobject]@{
			Width = [Math]::Max(1, $width)
			Height = [Math]::Max(1, $height)
		}
	}

	return $null
}

Add-Type -AssemblyName System.Drawing

Add-Type -ReferencedAssemblies System.Drawing -TypeDefinition @"
using System;
using System.Drawing.Imaging;
using System.IO;
using System.Runtime.InteropServices;

public static class NonPlaceableWmfRenderer {
	private const int BI_RGB = 0;
	private const int DIB_RGB_COLORS = 0;
	private const int MM_ANISOTROPIC = 8;
	private const int TRANSPARENT_SENTINEL = 0x00030201;
	private const int GDIPLUS_OK = 0;
	private const int PIXEL_FORMAT_32BPP_ARGB = 0x0026200A;
	private static readonly Guid PNG_ENCODER_CLSID = new Guid("557cf406-1a04-11d3-9a73-0000f81ef32e");

	[StructLayout(LayoutKind.Sequential)]
	private struct BITMAPINFOHEADER {
		public uint biSize;
		public int biWidth;
		public int biHeight;
		public ushort biPlanes;
		public ushort biBitCount;
		public uint biCompression;
		public uint biSizeImage;
		public int biXPelsPerMeter;
		public int biYPelsPerMeter;
		public uint biClrUsed;
		public uint biClrImportant;
	}

	[StructLayout(LayoutKind.Sequential)]
	private struct RGBQUAD {
		public byte rgbBlue;
		public byte rgbGreen;
		public byte rgbRed;
		public byte rgbReserved;
	}

	[StructLayout(LayoutKind.Sequential)]
	private struct BITMAPINFO {
		public BITMAPINFOHEADER bmiHeader;
		public RGBQUAD bmiColors;
	}

	[StructLayout(LayoutKind.Sequential)]
	private struct RECT {
		public int left;
		public int top;
		public int right;
		public int bottom;
	}

	[StructLayout(LayoutKind.Sequential)]
	private struct METAFILEPICT {
		public int mm;
		public int xExt;
		public int yExt;
		public IntPtr hMF;
	}

	[StructLayout(LayoutKind.Sequential)]
	private struct GDIPLUSSTARTUPINPUT {
		public uint GdiplusVersion;
		public IntPtr DebugEventCallback;
		public bool SuppressBackgroundThread;
		public bool SuppressExternalCodecs;
	}

	[DllImport("gdi32.dll", SetLastError = true)]
	private static extern IntPtr SetWinMetaFileBits(uint cbBuffer, byte[] lpbBuffer, IntPtr hdcRef, ref METAFILEPICT lpmfp);

	[DllImport("gdi32.dll", SetLastError = true)]
	private static extern bool DeleteEnhMetaFile(IntPtr hemf);

	[DllImport("gdi32.dll", SetLastError = true)]
	private static extern IntPtr CreateCompatibleDC(IntPtr hdc);

	[DllImport("gdi32.dll", SetLastError = true)]
	private static extern bool DeleteDC(IntPtr hdc);

	[DllImport("gdi32.dll", SetLastError = true)]
	private static extern IntPtr CreateDIBSection(IntPtr hdc, ref BITMAPINFO pbmi, uint usage, out IntPtr ppvBits,
			IntPtr hSection, uint offset);

	[DllImport("gdi32.dll", SetLastError = true)]
	private static extern IntPtr SelectObject(IntPtr hdc, IntPtr h);

	[DllImport("gdi32.dll", SetLastError = true)]
	private static extern bool DeleteObject(IntPtr ho);

	[DllImport("gdi32.dll", SetLastError = true)]
	private static extern int SetMapMode(IntPtr hdc, int iMode);

	[DllImport("gdi32.dll", SetLastError = true)]
	private static extern bool SetWindowOrgEx(IntPtr hdc, int x, int y, IntPtr lppt);

	[DllImport("gdi32.dll", SetLastError = true)]
	private static extern bool SetWindowExtEx(IntPtr hdc, int x, int y, IntPtr lpsz);

	[DllImport("gdi32.dll", SetLastError = true)]
	private static extern bool SetViewportOrgEx(IntPtr hdc, int x, int y, IntPtr lppt);

	[DllImport("gdi32.dll", SetLastError = true)]
	private static extern bool SetViewportExtEx(IntPtr hdc, int x, int y, IntPtr lpsz);

	[DllImport("gdi32.dll", SetLastError = true)]
	private static extern bool PlayEnhMetaFile(IntPtr hdc, IntPtr hemf, ref RECT lpRect);

	[DllImport("gdiplus.dll", ExactSpelling = true)]
	private static extern int GdiplusStartup(out IntPtr token, ref GDIPLUSSTARTUPINPUT input, IntPtr output);

	[DllImport("gdiplus.dll", ExactSpelling = true)]
	private static extern void GdiplusShutdown(IntPtr token);

	[DllImport("gdiplus.dll", ExactSpelling = true)]
	private static extern int GdipCreateBitmapFromScan0(int width, int height, int stride, int format, IntPtr scan0,
			out IntPtr bitmap);

	[DllImport("gdiplus.dll", ExactSpelling = true, CharSet = CharSet.Unicode)]
	private static extern int GdipSaveImageToFile(IntPtr image, string filename, ref Guid clsidEncoder,
			IntPtr encoderParams);

	[DllImport("gdiplus.dll", ExactSpelling = true)]
	private static extern int GdipDisposeImage(IntPtr image);

	public static void Render(string inputPath, string outputPath, int width, int height, bool transparent) {
		byte[] bits = File.ReadAllBytes(inputPath);
		METAFILEPICT pict = new METAFILEPICT();
		pict.mm = MM_ANISOTROPIC;
		pict.xExt = Math.Max(1, width) * 2540 / 144;
		pict.yExt = Math.Max(1, height) * 2540 / 144;

		IntPtr hemf = SetWinMetaFileBits((uint)bits.Length, bits, IntPtr.Zero, ref pict);
		if (hemf == IntPtr.Zero) {
			ThrowWin32("SetWinMetaFileBits");
		}

		IntPtr hdc = IntPtr.Zero;
		IntPtr bitmap = IntPtr.Zero;
		IntPtr oldBitmap = IntPtr.Zero;
		IntPtr pixelBits = IntPtr.Zero;
		try {
			hdc = CreateCompatibleDC(IntPtr.Zero);
			if (hdc == IntPtr.Zero) {
				ThrowWin32("CreateCompatibleDC");
			}
			bitmap = CreateSurface(hdc, width, height, out pixelBits);
			oldBitmap = SelectObject(hdc, bitmap);

			ClearArgb(pixelBits, width, height, transparent ? TRANSPARENT_SENTINEL : unchecked((int)0xFFFFFFFF));
			SetMapMode(hdc, MM_ANISOTROPIC);
			SetWindowOrgEx(hdc, 0, 0, IntPtr.Zero);
			SetWindowExtEx(hdc, width, height, IntPtr.Zero);
			SetViewportOrgEx(hdc, 0, 0, IntPtr.Zero);
			SetViewportExtEx(hdc, width, height, IntPtr.Zero);

			RECT rect = new RECT();
			rect.left = 0;
			rect.top = 0;
			rect.right = width;
			rect.bottom = height;
			if (!PlayEnhMetaFile(hdc, hemf, ref rect)) {
				ThrowWin32("PlayEnhMetaFile");
			}

			if (transparent) {
				ApplyTransparentAlpha(pixelBits, width, height, TRANSPARENT_SENTINEL);
			} else {
				ApplyOpaqueAlpha(pixelBits, width, height);
			}
			SaveBitmapAsPng(pixelBits, width, height, outputPath);
		} finally {
			if (oldBitmap != IntPtr.Zero) {
				SelectObject(hdc, oldBitmap);
			}
			if (bitmap != IntPtr.Zero) {
				DeleteObject(bitmap);
			}
			if (hdc != IntPtr.Zero) {
				DeleteDC(hdc);
			}
			DeleteEnhMetaFile(hemf);
		}
	}

	private static IntPtr CreateSurface(IntPtr hdc, int width, int height, out IntPtr bits) {
		BITMAPINFO bmi = new BITMAPINFO();
		bmi.bmiHeader.biSize = (uint)Marshal.SizeOf(typeof(BITMAPINFOHEADER));
		bmi.bmiHeader.biWidth = width;
		bmi.bmiHeader.biHeight = -height;
		bmi.bmiHeader.biPlanes = 1;
		bmi.bmiHeader.biBitCount = 32;
		bmi.bmiHeader.biCompression = BI_RGB;
		IntPtr bitmap = CreateDIBSection(hdc, ref bmi, DIB_RGB_COLORS, out bits, IntPtr.Zero, 0);
		if (bitmap == IntPtr.Zero) {
			ThrowWin32("CreateDIBSection");
		}
		return bitmap;
	}

	private static void ClearArgb(IntPtr bits, int width, int height, int argb) {
		int[] row = new int[width];
		for (int i = 0; i < row.Length; i++) {
			row[i] = argb;
		}
		for (int y = 0; y < height; y++) {
			Marshal.Copy(row, 0, IntPtr.Add(bits, y * width * 4), width);
		}
	}

	private static void ApplyTransparentAlpha(IntPtr bits, int width, int height, int transparentColor) {
		int transparentRgb = transparentColor & 0x00FFFFFF;
		int[] row = new int[width];
		for (int y = 0; y < height; y++) {
			IntPtr rowPtr = IntPtr.Add(bits, y * width * 4);
			Marshal.Copy(rowPtr, row, 0, width);
			for (int x = 0; x < width; x++) {
				if ((row[x] & 0x00FFFFFF) == transparentRgb) {
					row[x] = 0;
				} else {
					row[x] = unchecked((int)0xFF000000) | (row[x] & 0x00FFFFFF);
				}
			}
			Marshal.Copy(row, 0, rowPtr, width);
		}
	}

	private static void ApplyOpaqueAlpha(IntPtr bits, int width, int height) {
		int[] row = new int[width];
		for (int y = 0; y < height; y++) {
			IntPtr rowPtr = IntPtr.Add(bits, y * width * 4);
			Marshal.Copy(rowPtr, row, 0, width);
			for (int x = 0; x < width; x++) {
				row[x] = unchecked((int)0xFF000000) | (row[x] & 0x00FFFFFF);
			}
			Marshal.Copy(row, 0, rowPtr, width);
		}
	}

	private static void SaveBitmapAsPng(IntPtr bits, int width, int height, string outputPath) {
		IntPtr token = IntPtr.Zero;
		IntPtr image = IntPtr.Zero;
		GDIPLUSSTARTUPINPUT input = new GDIPLUSSTARTUPINPUT();
		input.GdiplusVersion = 1;
		int status = GdiplusStartup(out token, ref input, IntPtr.Zero);
		if (status != GDIPLUS_OK) {
			throw new InvalidOperationException("GdiplusStartup failed with status " + status);
		}

		try {
			status = GdipCreateBitmapFromScan0(width, height, width * 4, PIXEL_FORMAT_32BPP_ARGB, bits, out image);
			if (status != GDIPLUS_OK) {
				throw new InvalidOperationException("GdipCreateBitmapFromScan0 failed with status " + status);
			}

			Guid png = PNG_ENCODER_CLSID;
			status = GdipSaveImageToFile(image, outputPath, ref png, IntPtr.Zero);
			if (status != GDIPLUS_OK) {
				throw new InvalidOperationException("GdipSaveImageToFile failed with status " + status);
			}
		} finally {
			if (image != IntPtr.Zero) {
				GdipDisposeImage(image);
			}
			if (token != IntPtr.Zero) {
				GdiplusShutdown(token);
			}
		}
	}

	private static void ThrowWin32(string operation) {
		throw new System.ComponentModel.Win32Exception(Marshal.GetLastWin32Error(), operation + " failed");
	}
}
"@

$source = Convert-PathForWindows $InputPath
if (-not (Test-Path -LiteralPath $source)) {
	throw "Input file not found: $InputPath"
}

if ([string]::IsNullOrWhiteSpace($OutputPath)) {
	$outputName = [System.IO.Path]::ChangeExtension($source, ".png")
} else {
	$outputName = Convert-PathForWindows $OutputPath -ForOutput
}

$outputDir = Split-Path -Parent $outputName
if (-not [string]::IsNullOrWhiteSpace($outputDir) -and -not (Test-Path -LiteralPath $outputDir)) {
	New-Item -ItemType Directory -Path $outputDir | Out-Null
}

$image = $null
$bitmap = $null
$graphics = $null

try {
	$image = [System.Drawing.Image]::FromFile($source)
	$useNonPlaceablePlayEnh = Test-UseNonPlaceablePlayEnh $source $image

	if ($Width -le 0 -and $Height -le 0) {
		$canvasSize = Get-WmfCanvasSize $source $image.Width $image.Height
		if ($canvasSize -ne $null) {
			$Width = $canvasSize.Width
			$Height = $canvasSize.Height
		} else {
			$Width = [Math]::Max(1, $image.Width)
			$Height = [Math]::Max(1, $image.Height)
		}
	} elseif ($Width -le 0) {
		$Width = [Math]::Max(1, [int][Math]::Round($Height * $image.Width / $image.Height))
	} elseif ($Height -le 0) {
		$Height = [Math]::Max(1, [int][Math]::Round($Width * $image.Height / $image.Width))
	}

	if ($useNonPlaceablePlayEnh) {
		[NonPlaceableWmfRenderer]::Render($source, $outputName, $Width, $Height, $Transparent.IsPresent)
	} else {
		$bitmap = New-Object System.Drawing.Bitmap($Width, $Height, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
		$bitmap.SetResolution($Dpi, $Dpi)

		$graphics = [System.Drawing.Graphics]::FromImage($bitmap)
		$graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
		$graphics.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::AntiAliasGridFit
		$graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
		$graphics.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality

		if ($Transparent) {
			$graphics.Clear([System.Drawing.Color]::Transparent)
		} else {
			$graphics.Clear([System.Drawing.Color]::White)
		}

		$graphics.DrawImage($image, 0, 0, $Width, $Height)
		$bitmap.Save($outputName, [System.Drawing.Imaging.ImageFormat]::Png)
	}

	Write-Host ("{0} -> {1} ({2}x{3})" -f $source, $outputName, $Width, $Height)
} finally {
	if ($graphics -ne $null) {
		$graphics.Dispose()
	}
	if ($bitmap -ne $null) {
		$bitmap.Dispose()
	}
	if ($image -ne $null) {
		$image.Dispose()
	}
}

using System;
using System.Drawing;
using System.Drawing.Text;
 
class FontMetrics
{
﻿  static void Main(string[] args)
﻿  {
    InstalledFontCollection ifc = new InstalledFontCollection();
    foreach (FontFamily ff in ifc.Families) {
    	PrintEmHeight(ff.Name);
	}
﻿  }
﻿  
﻿  static void PrintEmHeight(String name) {
﻿  ﻿  FontFamily fontFamily = new FontFamily(name);
﻿  ﻿  int emheight =  fontFamily.GetEmHeight(FontStyle.Regular);
﻿  ﻿  int ascent = fontFamily.GetCellAscent(FontStyle.Regular);
﻿  ﻿  int descent = fontFamily.GetCellDescent(FontStyle.Regular);
﻿  ﻿  
﻿  ﻿  if (emheight != ascent + descent) {
﻿  ﻿  ﻿  Console.WriteLine("font-emheight." + name.Replace(" ", "\\u0020") + " =  " + (emheight / (double)(ascent + descent)));
﻿  ﻿  }
﻿  }
}

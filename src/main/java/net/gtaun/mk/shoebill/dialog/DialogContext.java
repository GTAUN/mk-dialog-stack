/**
 * Copyright (C) 2012 MK124
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as
 * published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */

package net.gtaun.mk.shoebill.dialog;

import net.gtaun.shoebill.constant.DialogStyle;
import net.gtaun.shoebill.object.Dialog;

/**
 * 对话框上下文类。
 * 
 * @author MK124
 */
public class DialogContext
{
	private Dialog dialog;
	private DialogStyle style;
	private String caption, text, button1, button2;
	
	
	public DialogContext(Dialog dialog, DialogStyle style, String caption, String text, String button1, String button2)
	{
		this.dialog = dialog;
		this.style = style;
		this.caption = caption;
		this.text = text;
		this.button1 = button1;
		this.button2 = button2;
	}
	
	public Dialog getDialog()
	{
		return dialog;
	}
	
	public DialogStyle getStyle()
	{
		return style;
	}
	
	public String getCaption()
	{
		return caption;
	}
	
	public String getText()
	{
		return text;
	}
	
	public String getButton1()
	{
		return button1;
	}
	
	public String getButton2()
	{
		return button2;
	}
}

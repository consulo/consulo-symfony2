package org.apache.commons.lang;

import com.intellij.openapi.util.text.StringUtil;

/**
 * @author VISTALL
 * @since 2019-03-16
 */
public class StringUtils
{
	public static boolean isBlank(String str)
	{
		return StringUtil.isEmptyOrSpaces(str);
	}

	public static boolean isNotBlank(String str)
	{
		return !StringUtil.isEmptyOrSpaces(str);
	}
}

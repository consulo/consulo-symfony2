package org.apache.commons.lang;

import com.intellij.openapi.util.text.StringUtil;

/**
 * @author VISTALL
 * @since 2019-03-16
 */
@Deprecated
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

	public static String trim(String str)
	{
		return StringUtil.trim(str);
	}

	public static String join(Iterable<?> iterable, String separator)
	{
		return StringUtil.join(iterable, separator);
	}

	public static String strip(String str, String chars)
	{
		return StringUtil.strip(str, c -> chars.length() == 1 && chars.charAt(0) == c);
	}
}

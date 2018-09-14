package com.sucre.weiboRegister.common;

/**
 * 存放一些公共变量
 * @author sucre
 *
 */
public class CommonVer {
	//接码平台的登录token
   public static String tokenCode;
   //短信平台的账号
   public static String otherId;
   //短信平台的密码
   public static String otherPassword;
   //宽带名称
   public static String ADSLname;
   //宽带账号
   public static String ADSLid;
   //宽带密码
   public static String ADSLpassword;
   //注册账号用的密码
   public static String registerPassword;
   //注册账号密码的明文
   public static String seeingPassword;
   //记录成功注册账号的数量,用来换ip
   public static int count=0;
}

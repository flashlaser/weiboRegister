package com.sucre.weiboRegister.function;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

import org.bouncycastle.jce.provider.JDKKeyFactory.RSA;

import com.sucre.weiboRegister.common.CommonVer;
import com.sucre.weiboRegister.mainUtil.MyUtil;
import com.sucre.weiboRegister.mainUtil.RsaUtils;
import com.sucre.weiboRegister.thread.Thread4Net;

/**
 * 主线程,负责建立线程,导入宽带连接等配置.
 * 
 * @author sucre
 *
 */
public class SinaWeiboRegister {
	public static Properties properties;

	/**
	 * 主线程main函数,args为初始化参数的数组 配置文件具体设置,参见readme.txt
	 * 
	 * @param args 参数顺序为: 0:宽带连接配置文件名称,
	 * 
	 */
	public static void main(String[] args) {
		// 导入配置信息
		String filename = args != null ? "info.properties" : args[0];
		loadip(filename);

		// 登录短信平台
		LoginOther loginother = new LoginOther(0, false);
		Thread thread = new Thread(loginother);
		thread.start();

		// MyUtil.sleeps(10000);
		// 开始注册
		RegisterImpl mythread = new RegisterImpl(0, true);
		Thread thread2 = new Thread(mythread);
		thread2.start();
		// RsaUtils.test("helloworld");
		System.out.println("程序开始运行.");
		//System.out.println(RsaUtils.encrypt("wqwqwq"));
	}

	public static void loadip(String filename) {
		properties = new Properties();
		File file = new File(filename);

		InputStream in = null;

		try {
			in = new FileInputStream(file);
			properties.load(in);
			CommonVer.otherId = properties.getProperty("otherId");
			CommonVer.otherPassword = properties.getProperty("otherPassword");
			CommonVer.ADSLname = properties.getProperty("ADSLname");
			CommonVer.ADSLid = properties.getProperty("ADSLid");
			CommonVer.ADSLpassword = properties.getProperty("ADSLpassword");
			CommonVer.registerPassword = properties.getProperty("registerPassword");
			CommonVer.seeingPassword = properties.getProperty("seeingPassword");
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
}

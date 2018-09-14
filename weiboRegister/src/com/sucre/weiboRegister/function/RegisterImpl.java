package com.sucre.weiboRegister.function;

import java.util.ArrayList;

import javax.swing.text.ChangedCharSetException;

import com.sucre.weiboRegister.common.CommonVer;
import com.sucre.weiboRegister.mainUtil.MyUtil;
import com.sucre.weiboRegister.mainUtil.RsaUtils;
import com.sucre.weiboRegister.net.Nets;
import com.sucre.weiboRegister.thread.Thread4Net;

public class RegisterImpl extends Thread4Net {

	public String mobile;
	public String imsi;

	protected RegisterImpl(int u, boolean isCircle) {
		super(u, isCircle);
	}

	public int doWork(int index) {
		
		Nets nets = new Nets();
		String ret; // = nets.goPost("weibo.com", 443, iniReg());
		String cookie = "";
		if (!MyUtil.isEmpty(CommonVer.tokenCode)) {
			// String hash = getHash(ret);
			// System.out.println(hash);
			// 短信平台取手机号
			ret = nets.GoHttp("www.51zggj.com", 9180, getMobile(CommonVer.tokenCode));
			if (!MyUtil.isEmpty(ret)) {
				mobile = MyUtil.midWord("hm=", "&", ret + "&");
				if (!MyUtil.isEmpty(mobile) && !mobile.startsWith("170")) {
					imsi = MyUtil.makeNumber(15);
					// mobile="19919930470";
					System.out.println("平台取得手机号:" + mobile + ",开始发送短信");
					// 发送短信
					ret = nets.goPost("api.weibo.cn", 443, sendSms(mobile, imsi));
					// ret = nets.goPost("weibo.com", 443, sendSms(mobile));
					if (!MyUtil.isEmpty(ret)) {
						// 发送成功
						if (ret.indexOf("验证码发送成功") != -1) {
							// 取短信内容
							long start = System.currentTimeMillis();
							String code = "";
							while (ret != "") {
								ret = nets.GoHttp("www.51zggj.com", 9180, getSms(CommonVer.tokenCode, mobile));
								if (!MyUtil.isEmpty(ret)) {
									System.out.println("平台收到" + mobile + ",内容为:" + MyUtil.midWord("\r\n\r\n","end",ret+"end"));
									final String m = "验证码：";
									if (ret.indexOf(m) != -1) {
										code = MyUtil.midWord(m, "（", ret).trim();
										// 先什么都不做,让循环退出自动下一步.
										ret = "";
									} else {
										// 超过80秒取不到短信,放弃.
										long ends = System.currentTimeMillis();
										if ((ends - start) > 80000 || ret.indexOf("\r\n1") == -1) {
											System.out.println("取信息超时或者号码有问题,放弃!");
											break;
										}
										// 继续循环取
										System.out.println("平台未收到短信:" + mobile + ",5秒钟后再取!"
												+ MyUtil.midWord("\r\n\r\n", "end", ret + "end"));
										MyUtil.sleeps(5000);
									}
								}
							}

							// 注册
							if (code != "") {
								ret = nets.goPost("api.weibo.cn", 443,
										regSubmit(mobile, CommonVer.registerPassword, imsi, code));
								if (!MyUtil.isEmpty(ret)) {
									// 注册成功
									if (ret.indexOf("注册成功") != -1) {
										System.out.println("注册成功,账号信息导出到<sinaId.txt>,开始设置密码!");
										String out = "";
										cookie = MyUtil.midWord(".weibo.com\":\"", "\",\"", ret);
										cookie="Set-Cookie: " + cookie.replaceAll("\\\\n", "Set-Cookie: ");
										cookie=MyUtil.getAllCookie(cookie);
										out = mobile + "|" + CommonVer.seeingPassword + "|" + imsi + "|"
												+ MyUtil.midWord("uid\":\"", "\",\"", ret) + "|"
												+ MyUtil.midWord("gsid\":\"", "\",\"", ret) + "|" + cookie + "|"
												+ MyUtil.midWord("oauth\":", "}}}", ret);
										MyUtil.outPutData("sinaId.txt", out);
										// 修改密码
										ChangeP(cookie);

									} else {
										System.out.println("注册失败!" + MyUtil.midWord("msg\":\"", "}", ret));
									}

								}
							}

						} else {
							System.out.println("短信发送失败!" + MyUtil.midWord("msg\":\"", "}", ret));
							// 拉黑号码,避免重复取到.
							ret = nets.GoHttp("www.51zggj.com", 9180, blockMobile(CommonVer.tokenCode, mobile));
						}
					}
				}
			}
			// 释放平台所有号码
			ret = nets.GoHttp("www.51zggj.com", 9180, releaseAll(CommonVer.tokenCode));
		} else {
			MyUtil.sleeps(2000);
		}
		return 0;
	}

	// 修改密码
	private void ChangeP(String cookie) {
		
		Nets nets = new Nets();
		String ret = nets.goPost("security.weibo.com", 443, iniPsd(cookie));
		if (!MyUtil.isEmpty(ret)) {
			cookie += MyUtil.getAllCookie(ret);
			ret = nets.goPost("security.weibo.com", 443, newpwd(cookie));
			if (!MyUtil.isEmpty(ret)) {
				ret = nets.goPost("security.weibo.com", 443, submitPassword(cookie, CommonVer.registerPassword));
                System.out.println("修改密码完成:"+MyUtil.midWord("msg\":\"", "}", ret));
                CommonVer.count++;
                //注册成功了三个,换ip
                if(CommonVer.count==3) {
                	System.out.println("正在换ip!");
                	MyUtil.cutAdsl(CommonVer.ADSLname);
                	MyUtil.sleeps(2000);
                	MyUtil.connAdsl(CommonVer.ADSLname, CommonVer.ADSLid, CommonVer.ADSLpassword);
                	System.out.println("换ip!完成");
                	CommonVer.count=0;
                }
			}

		}
	}

	private String getHash(String data) {
		String temp = MyUtil.midWord("password_tip", "info_list ", data);
		String ret = "";
		if (!MyUtil.isEmpty(temp)) {
			ArrayList<String> list = MyUtil.midWordAll("hidden\" name=\"", "\">", temp);
			if (!MyUtil.isEmpty(list)) {
				for (String t : list) {
					// a33820ecaa25b97ec4933f974949e7e0" value="affca7230756f88fefbe329a9d6895ec
					// t.replace("\" value=\"", "=");
					ret += "&" + t;
				}
			}
		}
		ret += "&regtime" + MyUtil.midWord("regtime", "\"/>", temp);
		ret += "&salttime" + MyUtil.midWord("salttime", "\"/>", temp);
		ret += "&sinaid" + MyUtil.midWord("sinaid", "\"/>", temp);
		ret = ret.replaceAll("\" value=\"", "=");

		return ret;
	}

	// 提交注册信息.
	private byte[] regSubmit(String phone, String password, String imsi, String code) {
		String temp = "smscode=" + code
				+ "&i=f842b7a&aid=01Anlv2XwdpcqURzkYptXmiLi9fN_RO1D8SsUEDj3POT1K0WY.&pwd=&flag=1&phone=" + phone +"&"
				+ "\r\n";
		StringBuilder data = new StringBuilder(900);

		data.append(
				"POST https://api.weibo.cn/2/register/by_phone?networktype=wifi&uicode=10000062&moduleID=701&wb_version=3654&getuser=1&c=android&i=f842b7a&ft=0&ua=HUAWEI-Che2-TL00__weibo__8.6.3__android__android4.4.2&wm=9006_2001&aid=01Anlv2XwdpcqURzkYptXmiLi9fN_RO1D8SsUEDj3POT1K0WY.&did=398bb3d034534767db4829445693fbca461d9b41&v_f=2&v_p=62&area=&from=1086395010&imei="
						+ imsi
						+ "&lang=zh_CN&skin=default&oldwm=9006_2001&sflag=1&luicode=10000760&getcookie=1&getoauth=1 HTTP/1.1\r\n");
		data.append("Host: api.weibo.cn\r\n");
		data.append("Connection: keep-alive\r\n");
		data.append("Content-Length: " + temp.length() + "\r\n");
		data.append("User-Agent: Che2-TL00_4.4.2_weibo_8.6.3_android\r\n");
		data.append("Content-Type: application/x-www-form-urlencoded\r\n");
		data.append("\r\n");
		data.append(temp);
		data.append("\r\n");
		data.append("\r\n");
		return data.toString().getBytes();
	}

	// 点击发送短信 api.weibo.cn
	private byte[] sendSms(String phone, String imsi) {
		String temp = "aid=01Anlv2XwdpcqURzkYptXmiLi9fN_RO1D8SsUEDj3POT1K0WY.&pwd=&flag=1&phone=" + phone + "&\r\n";
		StringBuilder data = new StringBuilder(900);

		data.append(
				"POST https://api.weibo.cn/2/register/sendcode?networktype=wifi&phone_id=0&uicode=10000760&moduleID=701&wb_version=3654&getuser=1&c=android&i=f842b7a&ft=0&ua=HUAWEI-Che2-TL00__weibo__8.6.3__android__android4.4.2&wm=9006_2001&aid=01Anlv2XwdpcqURzkYptXmiLi9fN_RO1D8SsUEDj3POT1K0WY.&did=398bb3d034534767db4829445693fbca461d9b41&v_f=2&v_p=62&area=&from=1086395010&imei="
						+ imsi + "&lang=zh_CN&skin=default&oldwm=9006_2001&phone=" + phone
						+ "&sflag=1&getcookie=1&getoauth=1 HTTP/1.1\r\n");
		data.append("Host: api.weibo.cn\r\n");
		data.append("Connection: keep-alive\r\n");
		data.append("Content-Length: " + temp.length() + "\r\n");
		data.append("User-Agent: Che2-TL00_4.4.2_weibo_8.6.3_android\r\n");
		data.append("Content-Type: application/x-www-form-urlencoded\r\n");
		data.append("\r\n");
		data.append(temp);
		data.append("\r\n");
		data.append("\r\n");
		data.append("\r\n");

		return data.toString().getBytes();
	}
	//取平台短信内容
	private byte[] getSms(String token, String phone) {
		StringBuilder data = new StringBuilder(900);
		data.append("GET http://www.51zggj.com/service.asmx/GetYzm2Str?token=" + token + "&xmid=4227&hm=" + phone
				+ "&sf=1 HTTP/1.1\r\n");
		data.append("Host: www.51zggj.com\r\n");
		data.append("Connection: keep-alive\r\n");
		data.append("Upgrade-Insecure-Requests: 1\r\n");
		data.append(
				"User-Agent: Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/56.0.2924.87 Safari/537.36\r\n");
		data.append("Accept: text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8\r\n");
		data.append("Referer: http://www.51kmf.com/osapijk.html\r\n");
		data.append("Accept-Language: zh-CN,zh;q=0.8\r\n");
		data.append("\r\n");
		data.append("\r\n");
		return data.toString().getBytes();
	}

	// 取修改密码要用的cookie
	private byte[] iniPsd(String c) {
		StringBuilder data = new StringBuilder(900);
		data.append("POST https://security.weibo.com/chgpwd/index HTTP/1.1\r\n");
		data.append("Host: security.weibo.com\r\n");
		data.append("Connection: keep-alive\r\n");
		data.append("Content-Length: 83\r\n");
		data.append("Accept: */*\r\n");
		data.append("Origin: https://security.weibo.com\r\n");
		data.append("X-Requested-With: XMLHttpRequest\r\n");
		data.append("x-wap-profile: http://wap1.huawei.com/uaprof/HONOR_Che2-TL00_UAProfile.xml\r\n");
		data.append(
				"User-Agent: Mozilla/5.0 (Linux; Android 4.4.2; Che2-TL00 Build/HonorChe2-TL00) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/30.0.0.0 Mobile Safari/537.36 Weibo (HUAWEI-Che2-TL00__weibo__8.6.3__android__android4.4.2)\r\n");
		data.append("Content-Type: application/x-www-form-urlencoded; charset=UTF-8\r\n");
		data.append(
				"Referer: https://security.weibo.com/account/security?aid=01Anlv2XwdpcqURzkYptXmiLgRWzrcNstB_lLIv9B-BX977D4.&from=1086395010&lang=zh_CN&skin=default&device_id=398bb3d034534767db4829445693fbca461d9b41&entry=client&sinainternalbrowser=topnav\r\n");
		data.append("Accept-Language: zh-CN,en-US;q=0.8\r\n");
		data.append(
				"Cookie: "+ c +"\r\n");
		data.append("\r\n");
		data.append("aid=01Anlv2XwdpcqURzkYptXmiLgRWzrcNstB_lLIv9B-BX977D4.&entry=client&from=1086395010\r\n");
		data.append("\r\n");
		data.append("\r\n");

		return data.toString().getBytes();
	}

	// 刷新cookie
	private byte[] newpwd(String c) {
		StringBuilder data = new StringBuilder(900);
		data.append("GET https://security.weibo.com/chgpwd/newpwd HTTP/1.1\r\n");
		data.append("Host: security.weibo.com\r\n");
		data.append("Connection: keep-alive\r\n");
		data.append("Accept: text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8\r\n");
		data.append("x-wap-profile: http://wap1.huawei.com/uaprof/HONOR_Che2-TL00_UAProfile.xml\r\n");
		data.append(
				"User-Agent: Mozilla/5.0 (Linux; Android 4.4.2; Che2-TL00 Build/HonorChe2-TL00) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/30.0.0.0 Mobile Safari/537.36 Weibo (HUAWEI-Che2-TL00__weibo__8.6.3__android__android4.4.2)\r\n");
		data.append(
				"Referer: https://security.weibo.com/account/security?aid=01Anlv2XwdpcqURzkYptXmiLgRWzrcNstB_lLIv9B-BX977D4.&from=1086395010&lang=zh_CN&skin=default&device_id=398bb3d034534767db4829445693fbca461d9b41&entry=client&sinainternalbrowser=topnav\r\n");
		data.append("Accept-Language: zh-CN,en-US;q=0.8\r\n");
		data.append("Cookie: " + c + "\r\n");
		data.append("X-Requested-With: com.sina.weibo\r\n");
		data.append("\r\n");
		data.append("\r\n");
		return data.toString().getBytes();
	}

	// 修改密码
	private byte[] submitPassword(String c, String password) {
		String temp = "newpwd=" + password + "&extra=1&\r\n";

		StringBuilder data = new StringBuilder(900);
		data.append("POST https://security.weibo.com/chgpwd/ajnewpwd HTTP/1.1\r\n");
		data.append("Host: security.weibo.com\r\n");
		data.append("Connection: keep-alive\r\n");
		data.append("Content-Length: " + temp.length() + "\r\n");
		data.append("Accept: */*\r\n");
		data.append("Origin: https://security.weibo.com\r\n");
		data.append("X-Requested-With: XMLHttpRequest\r\n");
		data.append("x-wap-profile: http://wap1.huawei.com/uaprof/HONOR_Che2-TL00_UAProfile.xml\r\n");
		data.append(
				"User-Agent: Mozilla/5.0 (Linux; Android 4.4.2; Che2-TL00 Build/HonorChe2-TL00) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/30.0.0.0 Mobile Safari/537.36 Weibo (HUAWEI-Che2-TL00__weibo__8.6.3__android__android4.4.2)\r\n");
		data.append("Content-Type: application/x-www-form-urlencoded; charset=UTF-8\r\n");
		data.append("Referer: https://security.weibo.com/chgpwd/newpwd\r\n");
		data.append("Accept-Language: zh-CN,en-US;q=0.8\r\n");
		data.append("Cookie: " + c + "\r\n");
		data.append("\r\n");
		data.append(temp);
		data.append("\r\n");
		data.append("\r\n");
		return data.toString().getBytes();
	}
	//取手机号
	private byte[] getMobile(String token) {
		StringBuilder data = new StringBuilder(900);
		data.append("GET http://www.51zggj.com/service.asmx/GetHM2Str?token=" + token
				+ "&xmid=4227&sl=1&lx=6&a1=&a2=&pk=&ks=0&rj=sucre12 HTTP/1.1\r\n");
		data.append("Host: www.51zggj.com\r\n");
		data.append("Connection: keep-alive\r\n");
		data.append("Upgrade-Insecure-Requests: 1\r\n");
		data.append(
				"User-Agent: Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/56.0.2924.87 Safari/537.36\r\n");
		data.append("Accept: text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8\r\n");
		data.append("Referer: http://www.51kmf.com/osapijk.html\r\n");
		data.append("Accept-Language: zh-CN,zh;q=0.8\r\n");
		data.append("\r\n");
		data.append("\r\n");
		return data.toString().getBytes();
	}
	//释放所有号码
	private byte[] releaseAll(String token) {
		StringBuilder data = new StringBuilder(900);
		data.append("GET http://www.51zggj.com/service.asmx/sfAllStr?token=" + token + " HTTP/1.1\r\n");
		data.append("Host: www.51zggj.com\r\n");
		data.append("Connection: keep-alive\r\n");
		data.append("Upgrade-Insecure-Requests: 1\r\n");
		data.append(
				"User-Agent: Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/56.0.2924.87 Safari/537.36\r\n");
		data.append("Accept: text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8\r\n");
		data.append("Referer: http://www.51kmf.com/osapijk.html\r\n");
		data.append("Accept-Language: zh-CN,zh;q=0.8\r\n");
		data.append("\r\n");
		data.append("\r\n");
		return data.toString().getBytes();
	}
	//把号码加入黑名单
	private byte[] blockMobile(String token,String phone) {
		StringBuilder data = new StringBuilder(900);
		data.append("GET http://www.51zggj.com/service.asmx/Hmd2Str?token="+ token +"&xmid=4227&hm="+ phone+"&sf=1 HTTP/1.1\r\n");
		data.append("Host: www.51zggj.com\r\n");
		data.append("Connection: keep-alive\r\n");
		data.append("Upgrade-Insecure-Requests: 1\r\n");
		data.append("User-Agent: Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/56.0.2924.87 Safari/537.36\r\n");
		data.append("Accept: text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8\r\n");
		data.append("Referer: http://www.51kmf.com/osapijk.html\r\n");
		data.append("Accept-Language: zh-CN,zh;q=0.8\r\n");
		data.append("\r\n");
		data.append("\r\n");
		return data.toString().getBytes();
	}
}

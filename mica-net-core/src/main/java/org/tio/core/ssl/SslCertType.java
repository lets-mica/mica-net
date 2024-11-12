package org.tio.core.ssl;

import org.tio.utils.hutool.StrUtil;

/**
 * ssl 证书类型
 *
 * @author L.cm
 */
public enum SslCertType {

	JKS("JKS", new String[]{".jks", ".keystore"}),
	PKCS12("PKCS12", new String[]{".p12", ".pfx"});

	/**
	 * 证书类型
	 */
	private final String type;
	/**
	 * 证书后缀名
	 */
	private final String[] exts;

	SslCertType(String type, String[] exts) {
		this.type = type;
		this.exts = exts;
	}

	public String getType() {
		return type;
	}

	public String[] getExts() {
		return exts;
	}

	/**
	 * 根据文件名判断证书类型
	 * @param fileName fileName
	 * @return SslCertType
	 */
	public static SslCertType from(String fileName) {
		if (StrUtil.isBlank(fileName)) {
			return SslCertType.JKS;
		}
		for (SslCertType certType : SslCertType.values()) {
			String[] exts = certType.getExts();
			for (String ext : exts) {
				if (fileName.toLowerCase().endsWith(ext)) {
					return certType;
				}
			}
		}
		return SslCertType.JKS;
	}

}

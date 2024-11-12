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
	private final String[] fileExtensions;

	SslCertType(String type, String[] fileExtensions) {
		this.type = type;
		this.fileExtensions = fileExtensions;
	}

	public String getType() {
		return type;
	}

	public String[] getFileExtensions() {
		return fileExtensions;
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
			String[] fileExtensions = certType.getFileExtensions();
			for (String fileExtension : fileExtensions) {
				if (fileName.toLowerCase().endsWith(fileExtension)) {
					return certType;
				}
			}
		}
		return SslCertType.JKS;
	}

}

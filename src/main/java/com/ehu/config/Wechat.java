package com.ehu.config;

import com.ehu.util.FileUtils;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

@Getter
@ConfigurationProperties(prefix = "pay.wechat")
public final class Wechat {
    public static final Integer DEFAULT_MCH = 1;

    private static final Wechat WECHAT_INSTANCE = new Wechat();

    private Wechat() {
    }

    public static Wechat getInstance() {
        return WECHAT_INSTANCE;
    }

    /**
     * appId
     * 应用：
     * 1.app序支付
     * 2.app序退款
     */
    private String appId;
    /**
     * 小程序appid
     * <p>
     * 应用：
     * 1.小程序支付
     * 2.小程序退款
     */
    private String appletsAppId;
    /**
     * 调用ip
     */
    private String spbillCreateIp;
    /**
     * 微信商户信息
     */
    private Map<Integer, WechatMch> mchMap;
    /**
     * 申请商户号的appid或商户号绑定的appid
     * 供微信转账使用
     */
    private Map<Integer, String> mchAppidMap;

    public void setAppId(String appId) {
        WECHAT_INSTANCE.appId = appId;
    }

    public void setAppletsAppId(String appletsAppId) {
        WECHAT_INSTANCE.appletsAppId = appletsAppId;
    }

    public void setSpbillCreateIp(String spbillCreateIp) {
        WECHAT_INSTANCE.spbillCreateIp = spbillCreateIp;
    }

    public void setMchMap(Map<Integer, WechatMch> mchMap) {
        WECHAT_INSTANCE.mchMap = mchMap;
    }

    public void setMchAppidMap(Map<Integer, String> mchAppidMap) {
        WECHAT_INSTANCE.mchAppidMap = mchAppidMap;
    }

    /**
     * 微信商户信息
     */
    @Getter
    public static class WechatMch {
        /**
         * 商户号
         */
        private String mchId;
        /**
         * 签名时使用
         */
        private String signKey;
        /**
         * 调用ssl接口时需要的ca证书
         * 应用：
         * 1.退款
         * 2.转账
         */
        private String ca;
        /**
         * ca证书的密码
         */
        private String caCode;
        /**
         * RSA加密的微信公钥
         * <p>
         * 应用：
         * 1. 转账给银行卡
         */
        private String publicKey;

        public void setMchId(String mchId) {
            this.mchId = mchId;
        }

        public void setSignKey(String signKey) {
            this.signKey = FileUtils.readFile(signKey);
        }

        public void setCa(String ca) {
            this.ca = FileUtils.readFile(ca);
        }

        public void setCaCode(String caCode) {
            this.caCode = FileUtils.readFile(caCode);
        }

        public void setPublicKey(String publicKey) {
            this.publicKey = FileUtils.readFile(publicKey);
        }
    }
}
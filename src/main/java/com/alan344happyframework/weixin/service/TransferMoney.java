package com.alan344happyframework.weixin.service;

import com.alan344happyframework.util.HttpClientUtils;
import com.alan344happyframework.util.RSAUtils;
import com.alan344happyframework.util.StringUtils;
import com.alan344happyframework.util.XmlUtils;
import com.alan344happyframework.util.bean.HttpParams;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.alan344happyframework.bean.PayResponse;
import com.alan344happyframework.bean.TransferMoneyInternal;
import com.alan344happyframework.config.Wechat;
import com.alan344happyframework.constants.PayBaseConstants;
import com.alan344happyframework.core.LowerUnderscoreFilter;
import com.alan344happyframework.core.httpresponsehandler.MapStringStringResponseHandler;
import com.alan344happyframework.core.responsehandler.WechatResponseHandler;
import com.alan344happyframework.exception.PayException;
import com.alan344happyframework.weixin.entity.TransferToBankCardParams;
import com.alan344happyframework.weixin.entity.WechatBusinessPay;
import com.alan344happyframework.weixin.util.Signature;
import com.alan344happyframework.weixin.util.WechatUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpException;

import java.io.IOException;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;


/**
 * @author AlanSun
 * @Date 2016年8月3日
 * 微信企业付款操作类
 */
@Slf4j
public class TransferMoney {

    private static final String BUSINESS_TRANSFER_URL = "https://api.mch.weixin.qq.com/mmpaymkttransfers/promotion/transfers";

    /**
     * 转账到银行卡
     */
    private static final String URL_TO_BANK_URL = "https://api.mch.weixin.qq.com/mmpaysptrans/pay_bank";

    /**
     * 查询转账到银行卡
     */
    private static final String QUERY_URL_TO_BANK_URL = "https://api.mch.weixin.qq.com/mmpaysptrans/query_bank";

    /**
     * RSA 算法
     */
    private static final String ALGORITHM = "RSA/ECB/OAEPWITHSHA-1ANDMGF1PADDING";

    /**
     * 微信转账到零钱
     *
     * @param params {@link WechatBusinessPay}
     * @return true or false
     * @throws PayException e
     */
    public static PayResponse<Map<String, String>> weChatPayBusinessPayForUser(TransferMoneyInternal params) throws PayException {
        WechatBusinessPay wechatBusinessPay = params.getWechatBusinessPay();
        int mchNo = wechatBusinessPay.getMchNo();
        int mchAppIdNo = wechatBusinessPay.getMchAppIdNo();

        Wechat config = Wechat.getInstance();
        String mchAppId = config.getMchAppIdMap().get(mchAppIdNo);
        Wechat.WechatMch wechatMch = config.getMchMap().get(mchNo);

        SortedMap<String, String> packageParams = new TreeMap<>();
        packageParams.put("mch_appid", mchAppId);
        packageParams.put("mchid", wechatMch.getMchId());
        packageParams.put("nonce_str", WechatUtils.getNonceStr());
        packageParams.put("partner_trade_no", params.getTransferId());
        packageParams.put("openid", params.getPayeeAccount());
        /*NO_CHECK：不校验真实姓名
        FORCE_CHECK：强校验真实姓名（未实名认证的用户会校验失败，无法转账）*/
        String reUserName = params.getReUserName();
        if (StringUtils.isEmpty(reUserName)) {
            packageParams.put("check_name", "NO_CHECK");
        } else {
            packageParams.put("re_user_name", reUserName);
            packageParams.put("check_name", "FORCE_CHECK");
        }

        packageParams.put("amount", WechatUtils.getFinalMoney(params.getAmount()));
        packageParams.put("spbill_create_ip", config.getSpbillCreateIp());
        packageParams.put("desc", params.getDesc());
        packageParams.put("sign", Signature.getSign(packageParams, wechatMch.getSignKey()));

        //发送得到微信服务器
        Map<String, String> responseMap = sendRequest(packageParams, wechatMch, BUSINESS_TRANSFER_URL);
        return WechatResponseHandler.getInstance().handler(responseMap, null);
    }

    /**
     * 转账到银行卡
     *
     * @param params {@link TransferToBankCardParams}
     * @return {@link PayResponse}
     */
    public static PayResponse<Map<String, String>> transferToBankCard(TransferToBankCardParams params) throws PayException {
        int mchNo = params.getMchNo();
        Wechat config = Wechat.getInstance();

        Wechat.WechatMch wechatMch = config.getMchMap().get(mchNo);

        String wxPublicKey = wechatMch.getPublicKey();
        try {
            params.setEncBankNo(RSAUtils.encryptByPublicKeyToString(params.getEncBankNo().getBytes(), wxPublicKey, ALGORITHM));
            params.setEncTrueName(RSAUtils.encryptByPublicKeyToString(params.getEncTrueName().getBytes(), wxPublicKey, ALGORITHM));
        } catch (Exception e) {
            PayResponse<Map<String, String>> response = new PayResponse<>();
            response.setResultCode(PayBaseConstants.RETURN_FAIL);
            response.setResultMessage("RSA error");
            return response;
        }

        String s = JSON.toJSONString(params, new LowerUnderscoreFilter());
        SortedMap<String, String> packageParams = JSON.parseObject(s, new TypeReference<TreeMap<String, String>>() {

        });
        packageParams.put("amount", WechatUtils.getFinalMoney(params.getAmount()));
        packageParams.put("mch_id", wechatMch.getMchId());
        packageParams.put("nonce_str", WechatUtils.getNonceStr());
        packageParams.put("sign", Signature.getSign(packageParams, wechatMch.getSignKey()));
        //发送得到微信服务器
        Map<String, String> responseMap = sendRequest(packageParams, wechatMch, URL_TO_BANK_URL);


        return WechatResponseHandler.getInstance().handler(responseMap, null);
    }

    /**
     * 查询企业付款到银行卡
     *
     * @param partnerTradeNo 商户订单号
     */
    public static PayResponse<Map<String, String>> getResultOfTransferToBank(String partnerTradeNo, int... mchNos) throws PayException {
        if (mchNos.length > 1) {
            throw new IllegalArgumentException("mchNo length must 1");
        }
        int mchNo = Wechat.DEFAULT_MCH;
        if (mchNos.length != 0) {
            mchNo = mchNos[0];
        }
        Wechat config = Wechat.getInstance();
        Wechat.WechatMch wechatMch = config.getMchMap().get(mchNo);

        SortedMap<String, String> packageParams = new TreeMap<>();
        packageParams.put("mch_id", wechatMch.getMchId());
        packageParams.put("partner_trade_no", partnerTradeNo);
        packageParams.put("nonce_str", WechatUtils.getNonceStr());
        packageParams.put("sign", Signature.getSign(packageParams, wechatMch.getSignKey()));
        //发送得到微信服务器
        Map<String, String> responseMap = sendRequest(packageParams, wechatMch, QUERY_URL_TO_BANK_URL);

        return WechatResponseHandler.getInstance().handler(responseMap, null);
    }

    private static Map<String, String> sendRequest(SortedMap<String, String> packageParams, Wechat.WechatMch wechatMch, String url) throws PayException {
        HttpParams httpParams = HttpParams.builder().url(url).strEntity(XmlUtils.mapToXml(packageParams)).build();
        try {
            return HttpClientUtils.doPostWithSslAndResponseHandler(wechatMch.getCa(), wechatMch.getCaCode(), httpParams, new MapStringStringResponseHandler());
        } catch (IOException | HttpException e) {
            if (log.isDebugEnabled()) {
                log.debug("转账失败", e);
            }
            throw new PayException("转账失败");
        }
    }
}

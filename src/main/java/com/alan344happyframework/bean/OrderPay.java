package com.alan344happyframework.bean;

import com.alan344happyframework.util.StringUtils;
import com.alipay.api.domain.AlipayTradeAppPayModel;
import com.alan344happyframework.core.Product;
import com.alan344happyframework.weixin.entity.WechatPayOrder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class OrderPay extends PayBase {
    /**
     * 订单id，必填
     */
    private String orderId;
    /**
     * 订单金额，必填
     */
    private String price;
    /**
     * 对一笔交易的具体描述信息。如果是多种商品，请将商品描述字符串累加传给body。
     * <p>
     * body 和 products 二者选其一，如果两个都填，使用body
     * <p>
     * 必填
     */
    private String body;
    /**
     * body 和 products 二者选其一，如果两个都填，使用body
     */
    private List<? extends Product> bodyProducts;
    /**
     * 商品的标题/交易标题/订单标题/订单关键字等。
     * <p>
     * 必填
     */
    private String subject;
    /**
     * 回调地址
     */
    private String notifyUrl;
    /**
     * 阿里其他参数
     */
    private AlipayTradeAppPayModel alipayTradeAppPayModel;
    /**
     * 微信其他参数
     */
    private WechatPayOrder wechatPayOrder = new WechatPayOrder();

    public void setBodyProducts(List<? extends Product> products) {
        if (StringUtils.isEmpty(this.body)) {
            this.body = Product.getNames(products);
        }
    }
}

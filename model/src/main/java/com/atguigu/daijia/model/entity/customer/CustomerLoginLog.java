package com.atguigu.daijia.model.entity.customer;

import com.atguigu.daijia.model.entity.base.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Schema(description = "CustomerLoginLog")
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CustomerLoginLog extends BaseEntity {

	private static final long serialVersionUID = 1L;

	@Schema(description = "客户id")
	private Long customerId;

	@Schema(description = "登录IP地址")
	private String ipaddr;

	@Schema(description = "登录状态（0成功 1失败）")
	private Boolean status;

	@Schema(description = "提示信息")
	private String msg;

}
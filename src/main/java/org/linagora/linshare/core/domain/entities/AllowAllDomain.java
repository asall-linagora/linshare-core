package org.linagora.linshare.core.domain.entities;

import org.linagora.linshare.core.domain.constants.DomainAccessRuleType;

public class AllowAllDomain extends DomainAccessRule {

	public AllowAllDomain() {
	}
	
	@Override
	public String toString() {
		return "My type is : " + String.valueOf(AllowAllDomain.class);
	}

	@Override
	public DomainAccessRuleType getDomainAccessRuleType() {
		return DomainAccessRuleType.ALLOW_ALL;
	}
	
}
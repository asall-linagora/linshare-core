/*
 * LinShare is an open source filesharing software, part of the LinPKI software
 * suite, developed by Linagora.
 * 
 * Copyright (C) 2016 LINAGORA
 * 
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Affero General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version, provided you comply with the Additional Terms applicable for
 * LinShare software by Linagora pursuant to Section 7 of the GNU Affero General
 * Public License, subsections (b), (c), and (e), pursuant to which you must
 * notably (i) retain the display of the “LinShare™” trademark/logo at the top
 * of the interface window, the display of the “You are using the Open Source
 * and free version of LinShare™, powered by Linagora © 2009–2015. Contribute to
 * Linshare R&D by subscribing to an Enterprise offer!” infobox and in the
 * e-mails sent with the Program, (ii) retain all hypertext links between
 * LinShare and linshare.org, between linagora.com and Linagora, and (iii)
 * refrain from infringing Linagora intellectual property rights over its
 * trademarks and commercial brands. Other Additional Terms apply, see
 * <http://www.linagora.com/licenses/> for more details.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Affero General Public License and
 * its applicable Additional Terms for LinShare along with this program. If not,
 * see <http://www.gnu.org/licenses/> for the GNU Affero General Public License
 * version 3 and <http://www.linagora.com/licenses/> for the Additional Terms
 * applicable to LinShare software.
 */

package org.linagora.linshare.mongo.entities.mto;

import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.linagora.linshare.core.domain.entities.Account;
import org.linagora.linshare.core.domain.entities.Guest;
import org.linagora.linshare.core.domain.entities.User;
import org.linagora.linshare.core.domain.objects.Recipient;

@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
public class AccountMto {

	protected String firstName;

	protected String name;

	protected String mail;

	protected String uuid;

	protected DomainMto domain;

	public AccountMto() {
	}

	public AccountMto(Account account) {
		this.name = account.getFullName();
		this.mail = account.getMail();
		this.uuid = account.getLsUuid();
		this.domain = new DomainMto(account.getDomain());
	}

	public AccountMto(User user) {
		this.name = user.getLastName();
		this.uuid = user.getLsUuid();
		this.mail = user.getMail();
		this.domain = new DomainMto(user.getDomain());
		this.firstName = user.getFirstName();
	}

	public AccountMto(Recipient recipient) {
		this.name = recipient.getLastName();
		this.uuid = recipient.getUuid();
		this.mail = recipient.getMail();
		this.domain = new DomainMto(recipient.getDomain());
		this.firstName = recipient.getFirstName();
	}

	public AccountMto(Guest guest) {
		this.name = guest.getLastName();
		this.firstName = guest.getFirstName();
		this.uuid = guest.getLsUuid();
		this.domain = new DomainMto(guest.getDomain());
		this.mail = guest.getMail();
	}

	public String getName() {
		return name;
	}

	public void setName(String lastName) {
		this.name = lastName;
	}

	public String getMail() {
		return mail;
	}

	public void setMail(String mail) {
		this.mail = mail;
	}

	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public DomainMto getDomain() {
		return domain;
	}

	public void setDomain(DomainMto domain) {
		this.domain = domain;
	}
}

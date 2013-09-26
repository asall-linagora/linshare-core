/*
 *    This file is part of Linshare.
 *
 *   Linshare is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as
 *   published by the Free Software Foundation, either version 3 of
 *   the License, or (at your option) any later version.
 *
 *   Linshare is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public
 *   License along with Foobar.  If not, see
 *                                    <http://www.gnu.org/licenses/>.
 *
 *   (c) 2008 Groupe Linagora - http://linagora.org
 *
*/
package org.linagora.linShare.core.domain.transformers.impl;

import java.util.ArrayList;
import java.util.List;

import org.linagora.linShare.core.domain.entities.Share;
import org.linagora.linShare.core.domain.entities.User;
import org.linagora.linShare.core.domain.transformers.Transformer;
import org.linagora.linShare.core.domain.vo.ShareDocumentVo;
import org.linagora.linShare.core.repository.ShareRepository;
import org.linagora.linShare.core.repository.UserRepository;

public class ShareTransformer implements Transformer<Share, ShareDocumentVo> {

	private final DocumentTransformer documentTransformer;
	
	private final ShareRepository shareRepository;
	
	private final UserRepository<User> userRepository;
	private final UserTransformer userTransformer;
	
	
	public ShareTransformer(final DocumentTransformer documentTransformer,
			final ShareRepository shareRepository,
			final UserRepository<User> userRepository,
			final UserTransformer userTransformer) {
		this.documentTransformer = documentTransformer;
		this.shareRepository = shareRepository;
		this.userRepository = userRepository;
		this.userTransformer = userTransformer;
	}

	public Share assemble(ShareDocumentVo shareDocumentVo) {
		
		Share theShare = shareRepository.getShare(documentTransformer.assemble(shareDocumentVo), 
				userRepository.findByLogin(shareDocumentVo.getSender().getLogin()),
				userRepository.findByLogin(shareDocumentVo.getReceiver().getLogin()));
		
		return theShare;
	
	}

	public List<Share> assembleList(List<ShareDocumentVo> valueObjectList) {
		List<Share> listResult = new ArrayList<Share>();
		for (ShareDocumentVo shareVo : valueObjectList) {
			listResult.add(assemble(shareVo));
		}
		return listResult;
		
	}

	public ShareDocumentVo disassemble(Share share) {
		return new ShareDocumentVo(documentTransformer.disassemble(share.getDocument()),
				userTransformer.disassemble(share.getSender()), userTransformer.disassemble(share.getReceiver()),
				share.getExpirationDate(), 
				share.getDownloaded(), share.getComment(), share.getSharingDate());
	}

	public List<ShareDocumentVo> disassembleList(List<Share> entityObjectList) {
		List<ShareDocumentVo> shareList = new ArrayList<ShareDocumentVo>();
		
		if (entityObjectList!=null) {
			for (Share share : entityObjectList) {
				shareList.add(disassemble(share));
			}
		}
		return shareList;
		
		
	}

}
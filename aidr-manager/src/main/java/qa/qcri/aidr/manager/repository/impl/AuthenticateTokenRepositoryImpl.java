package qa.qcri.aidr.manager.repository.impl;

import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;
import org.springframework.stereotype.Repository;
import qa.qcri.aidr.manager.hibernateEntities.AidrAuthenticateToken;
import qa.qcri.aidr.manager.repository.AuthenticateTokenRepository;

import java.io.Serializable;


/**
 * Created with IntelliJ IDEA.
 * User: jlucas
 * Date: 5/12/14
 * Time: 12:14 PM
 * To change this template use File | Settings | File Templates.
 */
@Repository("authenticateTokenRepository")
public class AuthenticateTokenRepositoryImpl extends GenericRepositoryImpl<AidrAuthenticateToken, Serializable> implements AuthenticateTokenRepository {

    @SuppressWarnings("unchecked")
    @Override
    public Boolean isAuthorized(String token) {

        Criteria criteria = getHibernateTemplate().getSessionFactory().getCurrentSession().createCriteria(AidrAuthenticateToken.class);
        criteria.add(Restrictions.eq("token", token));

        AidrAuthenticateToken aidrAuthenticateToken = (AidrAuthenticateToken) criteria.uniqueResult();
        return aidrAuthenticateToken != null;
    }
}

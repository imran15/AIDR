package qa.qcri.aidr.task.ejb.bean;

import org.hibernate.criterion.Restrictions;

import qa.qcri.aidr.task.ejb.CrisisService;
import qa.qcri.aidr.task.entities.Crisis;


import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.ejb.Stateless;

/**
 * 
 * @author Koushik
 *
 */
@Stateless(name="CrisisServiceBean")
public class CrisisServiceBean extends AbstractTaskManagerServiceBean<Crisis, Long> implements CrisisService {

    protected CrisisServiceBean(){
        super(Crisis.class);
    }

    @Override
    public Crisis findByCrisisID(Long id) {
        Crisis crisis = getByCriterionID(Restrictions.eq("crisisID",id));
        return crisis;  
    }

    @Override
    public List<Crisis> findByCriteria(String columnName, Long value) {
        return getAllByCriteria(Restrictions.eq(columnName,value))  ;
    }

    @Override
    public List<Crisis> findByCriteria(String columnName, String value) {
        return getAllByCriteria(Restrictions.eq(columnName,value))  ;
    }
}

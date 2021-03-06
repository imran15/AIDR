package qa.qcri.aidr.task.ejb.bean;

import org.hibernate.*;
import org.hibernate.criterion.*;
import org.hibernate.internal.CriteriaImpl;

import qa.qcri.aidr.task.ejb.AbstractTaskManagerService;

import java.io.Serializable;
import java.util.List;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;
import javax.persistence.Query;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 * 
 * @author Koushik
 *
 */
@Stateless(name = "AbstractTaskManagerServiceBean")
public class AbstractTaskManagerServiceBean<E, I extends Serializable> implements ServletContextListener, AbstractTaskManagerService<E,I> {

	@PersistenceContext(unitName = "qa.qcri.aidr.taskmanager-EJBS") 
	protected EntityManager em;
	//@PersistenceContext(unitName = "qa.qcri.aidr.taskmanager-EJBS") org.hibernate.Session session;

	//@PersistenceContext(unitName = "qa.qcri.aidr.taskmanager-EJBS") SessionFactory sessionFactory;


	private Class<E> entityClass;
	private SessionFactory sessionFactory;

	public AbstractTaskManagerServiceBean(Class<E> entityClass) {
		this.entityClass = entityClass;
		//getSessionFactory();
	}

	public AbstractTaskManagerServiceBean() {}

	@Override
	public EntityManager getEntityManager() {
		return em;
	}

	public Session getCurrentSession() {
		try { 
			return em.unwrap(Session.class);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	private SessionFactory getSessionFactory() {
		try {
			this.sessionFactory = em.unwrap(SessionFactory.class);
			return this.sessionFactory;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	public E getById(I id) {
		try {
			return (E) getCurrentSession().get(entityClass, id);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	@Override
	public E getByCriterionID(Criterion criterion) {
		try {
			Criteria criteria = getCurrentSession().createCriteria(entityClass);
			criteria.add(criterion);
			return (E) criteria.uniqueResult();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	public E getByCriteria(Criterion criterion) {
		try {
			Criteria criteria = getCurrentSession().createCriteria(entityClass);
			criteria.add(criterion);
			List<E> fetchedList = criteria.list();
			return (fetchedList != null) ? (E) fetchedList.get(0) : null;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	public List<E> getAll() {
		Criteria criteria = getCurrentSession().createCriteria(entityClass);
		//criteria.setProjection(Projections.distinct(Projections.property("id")));
		try {	
			List<E> fetchedList = criteria.list();
			return (fetchedList != null) ? (List<E>) fetchedList : null;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	public List<E> getAllByCriteria(Criterion criterion) {
		Criteria criteria = getCurrentSession().createCriteria(entityClass);
		criteria.add(criterion);
		try {	
			List<E> fetchedList = criteria.list();
			return (fetchedList != null) ? (List<E>) fetchedList : null;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	public List<E> getByCriteriaWithLimit(Criterion criterion, Integer count) {
		Criteria criteria = getCurrentSession().createCriteria(entityClass);
		criteria.add(criterion);
		if(count != null){
			criteria.setMaxResults(count);
		}
		try {	
			List<E> fetchedList = criteria.list();
			return (fetchedList != null) ? (List<E>) fetchedList : null;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	public List<E> getByCriteriaByOrder(Criterion criterion, String order, String[] orderBy, Integer count) {
		Criteria criteria = getCurrentSession().createCriteria(entityClass);
		criteria.add(criterion);
		for(int i = 0; i< orderBy.length; i++){
			if (order.equals("desc")) {
				criteria.addOrder(Order.desc(orderBy[i]));
			} else {
				criteria.addOrder(Order.asc(orderBy[i]));
			}
		}
		if(count != null){
			criteria.setMaxResults(count);
		}
		try {	
			List<E> fetchedList = criteria.list();
			return (fetchedList != null) ? (List<E>) fetchedList : null;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	public List<E> getByCriteriaWithAliasByOrder(Criterion criterion, String order, String[] orderBy, Integer count, String aliasTable, Criterion aliasCriterion) {
		Session session = getCurrentSession();
		System.out.println("[getByCriteriaWithAliasByOrder] entity: " + entityClass + ", current Session = " + session);
		Criteria criteria = session.createCriteria(entityClass);
		criteria.add(criterion);
		criteria.createAlias(aliasTable, aliasTable, CriteriaSpecification.LEFT_JOIN).add(aliasCriterion);
		if (orderBy != null) {
			for(int i = 0; i< orderBy.length; i++){
				if (order.equals("desc")) {
					criteria.addOrder(Order.desc(orderBy[i]));
				} else {
					criteria.addOrder(Order.asc(orderBy[i]));
				}
			}
		}
		if(count != null){
			criteria.setMaxResults(count);
		}
		//System.out.println("fetched List count = " + (fetchedList != null ? fetchedList.size() : null));
		try {	
			List<E> fetchedList = criteria.list();
			return (fetchedList != null) ? (List<E>) fetchedList : null;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public void update(E e) {
		try {
			Session session = getCurrentSession();
			session.saveOrUpdate(e);
			session.flush();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public void update(List<E> entityCollection) {
		Session session = getCurrentSession();
		Transaction tx = session.beginTransaction();
		for (E e: entityCollection) {
			session.saveOrUpdate(e);
			session.flush();
		}
		tx.commit();
	}

	public void save(E e) {
		try {
			Session session = getCurrentSession();
			session.save(e);
			session.flush();
		} catch (Exception ex) {
			ex.printStackTrace();
		}

	}

	public void merge(E e) {
		try {
			Session session = getCurrentSession();
			session.merge(e);
			session.flush();
		} catch (Exception ex) {
			ex.printStackTrace();
		}

	}

	public void merge(List<E> entityCollection) {
		Session session = getCurrentSession();
		Transaction tx = session.beginTransaction();
		for (E e: entityCollection) {
			session.merge(e);
			session.flush();
		}
		tx.commit();
	}

	public void save(List<E> entityCollection) {
		Session session = getCurrentSession();
		Transaction tx = session.beginTransaction();
		for (E e: entityCollection) {
			session.save(e);
			session.flush();
		}
		tx.commit();
	}

	public void delete(E e) {
		try {
			Session session = getCurrentSession();
			session.buildLockRequest(LockOptions.UPGRADE).lock(e);
			session.delete(e);
			session.flush();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public void delete(List<E> entityCollection) {
		Session session = getCurrentSession();
		Transaction tx = session.beginTransaction();
		for (E e: entityCollection) {
			session.buildLockRequest(LockOptions.UPGRADE).lock(e);
			session.delete(e);
			session.flush();
		}
		tx.commit();
	}

	public void deleteByCriteria(Criterion criterion) {
		try {
			List<E> entityCollection = getAllByCriteria(criterion);
			delete(entityCollection);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	@Override
	public void contextInitialized(ServletContextEvent sce) {
		// TODO Auto-generated method stub
	}

	@Override
	public void contextDestroyed(ServletContextEvent sce) {
		getCurrentSession().close();
		em.close();
	}
}

package com.heerkirov.bangumi.dao

import org.hibernate.Session
import org.hibernate.SessionFactory
import org.hibernate.cfg.Configuration
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import javax.annotation.Resource

@Component
class DatabaseEngine(@Resource(name = "sessionFactory") val factory: SessionFactory) {
    //private val factory = Configuration().configure().buildSessionFactory()!!
    fun session(): Session = factory.openSession()
    fun session(action:(Session) -> Unit) {
        val s = session()
        action(s)
        s.close()
    }
    fun<RET> session(action:(Session) -> RET): RET {
        val s = session()
        val ret = action(s)
        s.close()
        return ret
    }
}



import com.heerkirov.bangumi.dao.DatabaseEngine
import com.heerkirov.bangumi.model.Series
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class ModelInterfaceTest {
    @Test fun model() {
        val session = db!!.session()
        val cr = session.createCriteria(Series::class.java)
        val tx = session.beginTransaction()

        println("[1]")
        cr.setFirstResult(2).setMaxResults(3)
        cr.list().forEach { println(it?.toString()) }
        println("[2]")
        cr.list().forEach { println(it?.toString()) }

        tx.commit()
        session.close()
    }
    @Autowired val db: DatabaseEngine? = null
}
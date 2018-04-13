import com.heerkirov.bangumi.model.Anime
import com.heerkirov.bangumi.model.Author
import com.heerkirov.bangumi.model.Series
import org.hibernate.FetchMode
import org.hibernate.cfg.Configuration
import org.hibernate.criterion.CriteriaSpecification
import org.hibernate.criterion.MatchMode
import org.hibernate.criterion.Projections
import org.hibernate.criterion.Restrictions
import java.util.*

fun main(args: Array<String>){
    val factory = Configuration().configure().buildSessionFactory()!!
    val session = factory.openSession()
    val tx = session.beginTransaction()

    val anime = session.createCriteria(Anime::class.java)
            .setFetchMode("authorList", FetchMode.SELECT)
            .add(Restrictions.ilike("keyword", "1", MatchMode.ANYWHERE))
            .setResultTransformer(CriteriaSpecification.DISTINCT_ROOT_ENTITY)
            .list()
    println(anime)
    tx.commit()
    session.close()
}


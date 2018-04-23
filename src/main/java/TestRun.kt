import com.heerkirov.bangumi.model.Anime
import com.heerkirov.bangumi.model.Bangumi
import org.hibernate.FetchMode
import org.hibernate.cfg.Configuration
import org.hibernate.criterion.CriteriaSpecification

fun main(args: Array<String>){
    val factory = Configuration().configure().buildSessionFactory()!!
    val session = factory.openSession()
    val tx = session.beginTransaction()

    val anime = session.createCriteria(Bangumi::class.java)
            .setFetchMode("anime", FetchMode.SELECT)
            .setFetchMode("authorList", FetchMode.SELECT)
            .setFetchMode("tagList", FetchMode.SELECT)
            .setMaxResults(10)
            .setResultTransformer(CriteriaSpecification.DISTINCT_ROOT_ENTITY)
            .list()
    println("count: ${anime.size}")
    tx.commit()
    session.close()
}


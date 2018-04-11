import com.heerkirov.bangumi.model.Anime
import com.heerkirov.bangumi.model.Series
import org.hibernate.cfg.Configuration
import org.hibernate.criterion.Restrictions.*
import java.util.*

fun main(args: Array<String>){
    val factory = Configuration().configure().buildSessionFactory()!!
    val session = factory.openSession()
    val tx = session.beginTransaction()

    val series = session.createCriteria(Series::class.java).add(eq("id", 40)).uniqueResult() as Series

    val user = series.user!!

    val oldobj = series.animeList.first()
    val newobj = Anime(id = oldobj.id, name = "new obj animeRRR", uid = oldobj.uid, type = "novel", user= user, createTime = Calendar.getInstance(), updateTime = Calendar.getInstance())

    session.save(newobj)
    session.update(series)
    session.update(user)
    tx.commit()
    session.close()
}


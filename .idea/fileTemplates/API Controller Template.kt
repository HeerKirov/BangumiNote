#if (${PACKAGE_NAME} && ${PACKAGE_NAME} != "")package ${PACKAGE_NAME}
#end
import com.heerkirov.bangumi.converter.*
import com.heerkirov.bangumi.model.*
import com.heerkirov.bangumi.service.*
import org.hibernate.criterion.Restrictions.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.*
import javax.servlet.http.HttpServletRequest
#parse("File Header.java")
@Controller
@RequestMapping("/api/ "))
class ${NAME}(@Autowired private val security: Security,
              @Autowired private val request: HttpServletRequest): ApiController()) {
    override fun request(): HttpServletRequest = request
    override fun security(): Security = security

}
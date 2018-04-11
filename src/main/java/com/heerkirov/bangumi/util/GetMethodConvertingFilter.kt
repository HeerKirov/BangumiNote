package com.heerkirov.bangumi.util

import java.io.IOException
import javax.servlet.*
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletRequestWrapper


class GetMethodConvertingFilter : Filter {


    @Throws(ServletException::class)
    override fun init(config: FilterConfig) {
        // do nothing
    }


    @Throws(IOException::class, ServletException::class)
    override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
        chain.doFilter(wrapRequest(request as HttpServletRequest), response)
    }


    override fun destroy() {
        // do nothing
    }

    private fun wrapRequest(request: HttpServletRequest): HttpServletRequestWrapper {
        return object : HttpServletRequestWrapper(request) {
            override fun getMethod(): String {
                return "GET"
            }
        }
    }
}

package org.jboss.qa.tspresentation.web;

import java.io.IOException;
import java.io.PrintWriter;

import javax.annotation.Resource;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebServlet(name="WebTestServletBeanByResource", urlPatterns={"/stateful-resource"})
public class WebServletStatefulBeanByResource extends HttpServlet{
        private static final long serialVersionUID = 1L;
        private static final Logger log = LoggerFactory.getLogger(WebServletStatefulBeanByResource.class);

        @Resource
        private SessionStore store;

        @Override
        protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
            if(store.getPayload() == null) {
                store.setPayload(request.getRequestURI() + "-" + Math.random());
            }

            response.setContentType("text/html;charset=UTF-8");
            PrintWriter out = response.getWriter();
            out.print(store.getPayload());

            log.info("Session store count: " + SessionStore.INSTANCE_COUNT.get());
        }
}

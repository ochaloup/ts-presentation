package org.jboss.qa.tspresentation.web;

import java.io.IOException;
import java.io.PrintWriter;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(name="WebTestServletInject", urlPatterns={"/jpa"})
public class WebServletStartJpa extends HttpServlet{
    private static final long serialVersionUID = 1L;

    @Inject
    private StatefulExtendedJPABean xpc;

    @Inject
    private StatefulJPABean pc;

    @Override
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();

        if(request.getParameter("extended") != null) {
            if(request.getParameter("change") != null) {
                xpc.changeEntity();
            } else {
                xpc.createEntity();
            }
            out.println(xpc.findEntity().getName());
        } else {
            if(request.getParameter("change") != null) {
                pc.changeEntity();
            } else {
                pc.createEntity();
            }
            out.println(pc.findEntity().getName());
        }
    }
}

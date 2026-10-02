package com.example.appmgmt.web.servlet;

import com.example.appmgmt.common.BusinessException;
import com.example.appmgmt.domain.ImportBatch;
import com.example.appmgmt.domain.LoginUser;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

/** SC10 申込一括取込。 */
public class ImportServlet extends BaseServlet {

    @Override
    protected String fallbackPath(HttpServletRequest req) {
        return "/emp/import";
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        LoginUser user = requireUser(req);
        List<String> parts = pathParts(req);
        if (!parts.isEmpty()) {
            Long id = parseId(parts.get(0));
            ImportBatch b = id == null ? null : services().getImportService().findBatch(id).orElse(null);
            if (b == null || b.getImportEmployeeId() != user.getEmployeeId()) {
                res.sendError(404);
                return;
            }
            req.setAttribute("batch", b);
            req.setAttribute("errorsList", services().getImportService().findErrors(id));
        }
        req.setAttribute("history", services().getImportService().history(user));
        render(req, res, "emp/import.jsp");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        LoginUser user = requireUser(req);
        Part part;
        try {
            part = req.getPart("file");
        } catch (IllegalStateException e) {
            throw new BusinessException("E008");
        }
        if (part == null || part.getSize() == 0) {
            throw new BusinessException("E008");
        }
        String fileName = part.getSubmittedFileName();
        byte[] bytes;
        try (InputStream in = part.getInputStream()) {
            bytes = in.readAllBytes();
        }
        long id = services().getImportService().importCsv(user, fileName, bytes);
        flashMessage(req, "success", "I014");
        redirect(req, res, "/emp/import/" + id);
    }
}

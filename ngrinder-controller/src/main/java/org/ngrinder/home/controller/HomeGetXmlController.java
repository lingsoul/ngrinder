package org.ngrinder.home.controller;

import lombok.RequiredArgsConstructor;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;

@RestController
@RequestMapping("/home/api")
@RequiredArgsConstructor
public class HomeGetXmlController {

	@Autowired
	Environment environment;

	@RequestMapping(value = "/getXml", method = RequestMethod.GET)
	public void downloadFile(String xmlName, HttpServletRequest request, HttpServletResponse response) {

		String fileName = "localXmlFiles/" + xmlName;

		response.setCharacterEncoding(request.getCharacterEncoding());
		response.setContentType("application/xml");
		FileInputStream fis = null;
		try {
			ClassPathResource classPathResource = new ClassPathResource(fileName);
			InputStream inputStream = classPathResource.getInputStream();
			try {
				IOUtils.copy(inputStream, response.getOutputStream());
				response.flushBuffer();

			} finally {
				IOUtils.closeQuietly(inputStream);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (fis != null) {
				try {
					fis.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
}

/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ngrinder.home.service;

import com.sun.syndication.feed.synd.SyndEntryImpl;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.ngrinder.home.model.PanelEntry;
import org.ngrinder.infra.config.UserDefinedMessageSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

import static org.ngrinder.common.constant.CacheConstants.CACHE_LEFT_PANEL_ENTRIES;
import static org.ngrinder.common.constant.CacheConstants.CACHE_RIGHT_PANEL_ENTRIES;
import static org.ngrinder.common.util.TypeConvertUtils.cast;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
/**
 * nGrinder index page data retrieval service.
 *
 * @since 3.1
 */
@Component
@RequiredArgsConstructor
public class HomeService {
	private static final int PANEL_ENTRY_SIZE = 8;

	private static final Logger LOG = LoggerFactory.getLogger(HomeService.class);

	private final UserDefinedMessageSource userDefinedMessageSource;

	@Autowired
	Environment environment;

	/**
	 * Get the let panel entries from the given feed RUL.
	 *
	 * @param feedURL feed url
	 * @return the list of {@link PanelEntry}
	 */
	@SuppressWarnings("unchecked")
	@Cacheable(CACHE_LEFT_PANEL_ENTRIES)
	public List<PanelEntry> getLeftPanelEntries(String feedURL) {
		return getPanelEntries(feedURL, PANEL_ENTRY_SIZE, false);
	}

	/**
	 * Get the right panel entries containing the entries from the given RSS
	 * url.
	 *
	 * @param feedURL rss url message
	 * @return {@link PanelEntry} list
	 */
	@Cacheable(CACHE_RIGHT_PANEL_ENTRIES)
	public List<PanelEntry> getRightPanelEntries(String feedURL) {
		return getPanelEntries(feedURL, PANEL_ENTRY_SIZE, true);
	}

	public Map<String, String> getUserDefinedMessageSources(String locale) {
		return userDefinedMessageSource.getMessageSourcesByLocale().get(locale);
	}

	/**
	 * Get panel entries containing the entries from the given RSS
	 * url.
	 *
	 * @param feedURL      rss url message
	 * @param maxSize      max size
	 * @param includeReply if including reply
	 * @return {@link PanelEntry} list
	 */
	public List<PanelEntry> getPanelEntries(String feedURL, int maxSize, boolean includeReply) {
		SyndFeedInput input = new SyndFeedInput();
		XmlReader reader = null;
		HttpURLConnection feedConnection = null;
		try {
			List<PanelEntry> panelEntries = new ArrayList<>();
			if(feedURL.contains("local")){
				feedURL = getXmlUrl(feedURL);
			}
			URL url = new URL(feedURL);
			feedConnection = (HttpURLConnection) url.openConnection();
			feedConnection.setConnectTimeout(8000);
			feedConnection.setReadTimeout(8000);
			reader = new XmlReader(feedConnection);
			SyndFeed feed = input.build(reader);
			int count = 0;

			for (Object eachObj : feed.getEntries()) {
				SyndEntryImpl each = cast(eachObj);
				if (!includeReply && StringUtils.startsWithIgnoreCase(each.getTitle(), "Re: ")) {
					continue;
				}
				if (count++ >= maxSize) {
					break;
				}
				panelEntries.add(getPanelEntry(each));
			}
			Collections.sort(panelEntries);
			return panelEntries;
		} catch (Exception e) {
			LOG.error("Error while patching the feed entries for {} : {}", feedURL, e.getMessage());
		} finally {
			if (feedConnection != null) {
				feedConnection.disconnect();
			}
			IOUtils.closeQuietly(reader);
		}
		return Collections.emptyList();
	}

	private PanelEntry getPanelEntry(SyndEntryImpl each) {
		PanelEntry entry = new PanelEntry();
		entry.setAuthor(each.getAuthor());
		entry.setLastUpdatedDate(each.getUpdatedDate() == null ? each.getPublishedDate() : each
				.getUpdatedDate());
		if (each.getTitle() == null) {
			String[] split = StringUtils.split(each.getLink(), "/");
			entry.setTitle(split[split.length - 1].replace("-", " "));
		} else {
			entry.setTitle(each.getTitle());
		}
		if (StringUtils.startsWith(each.getLink(), "http")) {
			entry.setLink(each.getLink());
		} else {
			String uri = each.getUri();
			if (isGithubWiki(uri)) {
				uri = uri.substring(0, StringUtils.lastIndexOf(uri, "/"));
			}
			entry.setLink(uri);
		}
		return entry;
	}

	private boolean isGithubWiki(String uri) {
		return StringUtils.startsWith(uri, "https://github.com") && StringUtils.contains(uri, "/wiki/");
	}

	private String getXmlUrl(String feedURL) {
		String fileName = "ngrinder-user-cn-f114.xml";
		if("local.wiki".equals(feedURL)){
			fileName = "wiki.atom";
		}
		String xmlUrl = "http://127.0.0.1:" + getPort() + "/home/api/getXml?xmlName=" + fileName;
		return xmlUrl;
	}

	public String getPort(){
		return environment.getProperty("local.server.port");
	}
}

package com.harystolho.page;

import java.net.URI;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import com.harystolho.Main;
import com.harystolho.controllers.TaskController;

import javafx.application.Platform;
import javafx.concurrent.Worker.State;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.paint.Color;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Callback;

public class PageDownloader {

	private static final Logger logger = Logger.getLogger(PageDownloader.class.getName());

	private String url;

	private Document page;

	public PageDownloader(String url) {
		this.url = url;
	}

	/**
	 * Downloads the web page, lists all the nodes and put them in the
	 * {@link TaskController} ListView.
	 */
	private void afterPageDownload() {
		getAllElements(page.select("head").get(0));
		getAllElements(page.select("body").get(0));
	}

	private void getAllElements(Element e) {

		if (filterTag(e.tagName())) {
			CustomTag tag = new CustomTag(handleHtmlTag(e.outerHtml()), e.cssSelector());
			// Add element's classes
			tag.addClasses(e.classNames());
			// Add element's id
			tag.setId(e.id());
			// Adds the element to the list
			Main.getGUI().getTaskController().addTagToSelectorList(tag);
		}

		if (e.children().size() > 0) {
			for (Element child : e.children()) {
				getAllElements(child);
			}
		}

	}

	/**
	 * Filter Nodes.
	 * 
	 * @param tagName
	 *            the tag
	 * @return true if the node is allowed
	 */
	private boolean filterTag(String tagName) {
		// TODO Tag Filter
		return true;
	}

	private String handleHtmlTag(String outerHtml) {
		return StringUtils.splitPreserveAllTokens(outerHtml, ">")[0] + ">";
	}

	public void downloadPage(boolean show) {
		downloadPage(show, (param) -> {
		});
	}

	/**
	 * Creates a {@link WebView} object and loads the webpage using {@link #url} and
	 * waits for the user to close the window.
	 * 
	 */
	public void downloadPage(boolean show, Consumer<Document> callback) {

		if (url == null) {
			showAlert("Invalid URL", "The URL is not valid");
			logger.log(Level.INFO, "Can't download page because the URL is null");
			return;
		}

		try {
			new URI(url);
		} catch (Exception e) {
			showAlert("Invalid URL", "The URL provided is not valid.");
			return;
		}

		Platform.runLater(() -> {

			WebView view = new WebView();
			Stage stage = new Stage();

			stage.setTitle("Close this window when the page has loaded.");
			stage.setMaxWidth(1280);
			stage.setMaxHeight(720);

			Scene sc = new Scene(view);

			stage.setScene(sc);

			view.getEngine().getLoadWorker().stateProperty().addListener((obs, oldValue, newValue) -> {

				if (newValue == State.RUNNING) {
					if (show) {
						stage.show();
					} else {
						stage.show();
						stage.toBack();
					}
				}

				if (newValue == State.SUCCEEDED) {
					// If the window is shown to the user
					if (show) {
						Alert alert = new Alert(AlertType.CONFIRMATION);

						alert.setTitle("Info");
						alert.setContentText("Close the window below when the page has finished loading.");
						alert.show();

						stage.setOnCloseRequest((e) -> {
							page = Jsoup.parse(
									(String) view.getEngine().executeScript("document.documentElement.outerHTML"));
							afterPageDownload();
						});
					} else {

						// TODO add custom delay

						stage.close();
						page = Jsoup
								.parse((String) view.getEngine().executeScript("document.documentElement.outerHTML"));
						callback.accept(page);
					}
				}

			});

			view.getEngine().load(url);

		});

	}

	private void showAlert(String title, String msg) {
		Platform.runLater(() -> {
			Alert alert = new Alert(AlertType.ERROR);
			alert.setTitle(title);
			alert.setContentText(msg);
			alert.show();
		});
	}

	public Document getDocument() {
		return page;
	}

}

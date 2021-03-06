package com.harystolho.controllers;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import org.apache.commons.lang3.StringUtils;

import com.harystolho.Main;
import com.harystolho.page.CustomTag;
import com.harystolho.page.PageDownloader;
import com.harystolho.task.Task;
import com.harystolho.task.TaskUnits;
import com.harystolho.task.TaskUtils;
import com.harystolho.utils.ViwksUtils;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.FlowPane;

public class TaskController implements Controller {

	@FXML
	private TextField urlField;

	@FXML
	private Button loadPageButton;

	@FXML
	private FlowPane flowPane;

	@FXML
	private TextField selectorField;

	@FXML
	private TextField intervalField;

	@FXML
	private MenuButton unitButton;

	@FXML
	private TextArea valueText;

	@FXML
	private MenuButton valueSelectorButton;

	@FXML
	private TextField taskNameField;

	@FXML
	private Button saveButton;

	@FXML
	private Button closeButton;

	@FXML
	private ToggleButton enableClassButton;

	@FXML
	private ToggleButton enableIdButton;

	@FXML
	private TextField listFilter;

	@FXML
	private ListView<CustomTag> tagList;
	private List<CustomTag> temp;

	private String filterText = "";

	private Task currentTask;

	private PageDownloader page;

	@FXML
	void initialize() {
		Main.getGUI().setTaskController(this);

		temp = new ArrayList<>();

		if (currentTask == null)
			currentTask = createDefaultTask();

		loadTask();

		loadEventListeners();
	}

	/**
	 * Adds Event Listeners for this View
	 */
	private void loadEventListeners() {

		filterKeyPress();
		closeMouseClick();

		saveButton.setOnMouseClicked((e) -> {

			updateTask();
			TaskUtils.saveTask(currentTask);

			ViwksUtils.addCssEffect(saveButton, "button-pressed", 250);

		});

		loadPageButton.setOnMouseClicked((e) -> {

			ViwksUtils.getExecutor().submit(() -> {
				downloadPage();
			});
		});

		unitButton.getItems().forEach((item) -> {
			item.setOnAction((e) -> {
				unitButton.setText(((MenuItem) e.getTarget()).getText());
			});
		});

		valueSelectorButton.getItems().forEach((item) -> {
			item.setOnAction((e) -> {
				valueSelectorButton.setText(((MenuItem) e.getTarget()).getText());
				displayTagValue(tagList.getSelectionModel().getSelectedItem());
			});
		});

		tagList.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
			displayClassesAndId((CustomTag) newValue);
			displayTagValue((CustomTag) newValue);
			displayCssSelector((CustomTag) newValue);
		});

		enableClassButton.setOnAction((e) -> {
			filter();
		});

		enableIdButton.setOnAction((e) -> {
			filter();
		});

	}

	/**
	 * Displays this tag's value or innerHTML
	 * 
	 * @param tag
	 */
	private void displayTagValue(CustomTag tag) {

		if (tag == null) {
			return;
		}

		String displayValue = "";

		// Edit RunUtils when you edit here
		switch (valueSelectorButton.getText()) {
		case "value":
			displayValue = page.getDocument().select(tag.getCssSelector()).val();
			break;
		case "innerHTML":
			displayValue = page.getDocument().select(tag.getCssSelector()).text();
			break;
		default:
			break;
		}

		valueText.setText(displayValue);

	}

	/**
	 * Displays information about this tag's classes and id
	 * 
	 * @param tag
	 */
	private void displayClassesAndId(CustomTag tag) {

		flowPane.getChildren().clear();

		if (tag == null) {
			return;
		}

		for (String s : tag.getClasses()) {
			Label text = new Label(s);
			text.getStyleClass().add("flowPaneText");
			flowPane.getChildren().add(text);
		}

		Label text = new Label(tag.getId());
		text.getStyleClass().add("flowPaneText");
		flowPane.getChildren().add(text);

	}

	private void displayCssSelector(CustomTag tag) {
		selectorField.setText(tag.getCssSelector());
	}

	/**
	 * Downloads a web page using the URL in the {@link #urlField}
	 */
	private void downloadPage() {

		Platform.runLater(() -> {
			tagList.getItems().clear();
		});

		// TODO check if the URL is valid
		if (isURLValid(urlField.getText())) {
			page = new PageDownloader(urlField.getText());
		} else {
			Alert alert = new Alert(AlertType.ERROR);
			alert.setHeaderText("Invalid URL");
			alert.setContentText(urlField.getText() + " is not a valid URL");
			alert.showAndWait();
			return;
		}

		loadPageButton.setDisable(true);

		page.downloadPage(true);

		loadPageButton.setDisable(false);

	}

	/**
	 * Adds the tag to the list on the left side of the screen
	 * 
	 * @param tag
	 */
	public void addTagToSelectorList(CustomTag tag) {
		Platform.runLater(() -> {
			tagList.getItems().add(tag);
		});
	}

	/**
	 * Updates the {@link #currentTask} object using fields in the gui
	 */
	private void updateTask() {

		try {
			currentTask.setURL(new URL(urlField.getText()));
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}

		currentTask.setName(taskNameField.getText());
		currentTask.setCssSelector(selectorField.getText());
		currentTask.setInterval(Integer.valueOf(intervalField.getText()));
		currentTask.setUnit(getUnitButtonUnit());
		currentTask.setDisplaySelector(valueSelectorButton.getText());

		if (enableClassButton.isSelected()) {
			currentTask.getConfigs().put(Task.conf.ENABLE_CLASS, true);
		} else {
			currentTask.getConfigs().put(Task.conf.ENABLE_CLASS, false);
		}

		if (enableIdButton.isSelected()) {
			currentTask.getConfigs().put(Task.conf.ENABLE_ID, true);
		} else {
			currentTask.getConfigs().put(Task.conf.ENABLE_ID, false);
		}

	}

	private TaskUnits getUnitButtonUnit() {

		switch (unitButton.getText()) {
		case "Second(s)":
			return TaskUnits.SECOND;
		case "Minute(s)":
			return TaskUnits.MINUTE;
		case "Hour(s)":
			return TaskUnits.HOUR;
		case "Day(s)":
			return TaskUnits.DAY;
		default:
			return TaskUnits.MINUTE;
		}

	}

	/**
	 * Creates a new task with default configuration
	 * 
	 * @return {@link com.harystolho.task.Task Task}
	 */
	public Task createDefaultTask() {

		Task task = new Task.TaskBuilder().build();

		return task;

	}

	/**
	 * Loads data from {@link #currentTask} and displays it in the application
	 */

	public void loadTask() {
		loadTask(currentTask);
	}

	public void loadTask(Task task) {

		if (task.getURL() != null) {
			urlField.setText(task.getURL().toString());
		} else {
			urlField.setText("https://");
		}
		// Moves the cursor to the end of the string
		urlField.selectEnd();
		urlField.forward();

		taskNameField.setText(task.getName());

		selectorField.setText(task.getCssSelector());

		valueSelectorButton.setText(task.getDisplaySelector());

		intervalField.setText(task.getInterval() + "");

		unitButton.setText(task.getUnit().getName());

		valueSelectorButton.setText(task.getDisplaySelector());

		if ((boolean) task.getConfigs().get(Task.conf.ENABLE_CLASS)) {
			enableClassButton.setSelected(true);
		} else {
			enableClassButton.setSelected(false);
		}

		if ((boolean) task.getConfigs().get(Task.conf.ENABLE_ID)) {
			enableIdButton.setSelected(true);
		} else {
			enableIdButton.setSelected(false);
		}

		currentTask = task;

	}

	/**
	 * Filter tags using text and filter options. At the moment it uses two arrays
	 * to store the elements, one stores the elements being displayed and the other
	 * one the ones that don't match the filter. Every time this method is called,
	 * it iterates over both arrays.
	 */
	private void filter() {
		boolean enableClass = enableClassButton.isSelected();
		boolean enableId = enableIdButton.isSelected();

		// TODO improve the filter to use only 1 list, if possible, try using LinkedList
		ListIterator<CustomTag> iterator = tagList.getItems().listIterator();

		while (iterator.hasNext()) {
			CustomTag tag = iterator.next();

			if (tag != null) {

				if (StringUtils.contains(tag.getOuterHtml(), listFilter.getText())) {

					// If enableClass is OFF and the tag has at least one class, then it is removed.
					if (!enableClass && !tag.getClasses().isEmpty()) {
						temp.add(tag);
						iterator.remove();
						continue;
					}

					// If enableId is OFF and the tag has an id, then it's removed.
					if (!enableId && tag.getId() != null) {
						temp.add(tag);
						iterator.remove();
						continue;
					}

				} else { // If it doesn't match the filter, then it's removed.
					temp.add(tag);
					iterator.remove();
				}
			}
		}

		iterator = temp.listIterator();

		while (iterator.hasNext()) {
			CustomTag tag = iterator.next();

			if (tag != null) {
				if (StringUtils.contains(tag.getOuterHtml(), listFilter.getText())) {

					// If enableClass is ON and the tag has at least 1 class, then it's added back
					// to the list
					if (enableClass && !tag.getClasses().isEmpty()) {
						tagList.getItems().add(tag);
						iterator.remove();
						continue;
					}

					// If the tag has no classes and no id, it's added back to the list
					if (tag.getClasses().isEmpty() && tag.getId() == null) {
						tagList.getItems().add(tag);
						iterator.remove();
						continue;
					}

					// If enableId is ON and the tag has an id, it's added back to the list
					if (enableId && tag.getId() != null) {
						tagList.getItems().add(tag);
						iterator.remove();
						continue;
					}

				}
			}
		}
	}

	private void closeMouseClick() {
		closeButton.setOnMouseClicked((e) -> {
			Main.getGUI().getMainController().setCurrentTask(currentTask);
			// Loads new tasks in the main stage
			Main.getGUI().getMainController().loadTasks();
			// Sets this Controller's reference to null
			Main.getGUI().setTaskController(null);
			// Change to main Scene
			Main.getGUI().setScene(Main.getGUI().getMainScene());
		});
	}

	private void filterKeyPress() {
		listFilter.setOnKeyPressed((e) -> {
			filter();
		});

	}

	public void setTask(Task task) {
		currentTask = task;
	}

	public Task getTask() {
		return currentTask;
	}

	/**
	 * Checks if it URL is valid.
	 * 
	 * @param url
	 * @return true if it's valid
	 */
	private boolean isURLValid(String url) {
		return true;
	}

}

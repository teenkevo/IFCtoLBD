/*
 * The GNU Affero General Public License
 * 
 * Copyright (c) 2018 Jyrki Oraskari (Jyrki.Oraskari@aalto.fi / jyrki.oraskari@aalto.fi)
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

/*
 * To compile this, Java 8 is needed. jfxrt.jar is included, so, the the plugin should not be mandatory
 * but installing the http://www.eclipse.org/efxclipse/index.html and http://gluonhq.com/open-source/scene-builder/
 * make coding easier. 
 * 
   Royalty Free Stock Image: Blue Glass web icons, buttons
   The File image is implemented using:
   http://www.dreamstime.com/royalty-free-stock-image-blue-glass-web-icons-buttons-image8270526
 */

package org.lbd.ifc2lbd;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.controlsfx.control.MaskerPane;
import org.controlsfx.control.ToggleSwitch;
import org.controlsfx.control.textfield.CustomTextField;
import org.lbd.ifc2lbd.messages.ProcessReadyEvent;
import org.lbd.ifc2lbd.messages.SystemStatusEvent;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuBar;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class IFCtoLBDController implements Initializable, FxInterface {
	private static String ontologyNamespace;
	private final EventBus eventBus = EventBusService.getEventBus();
	private ExecutorService executor = Executors.newFixedThreadPool(1);

	@FXML
    private AnchorPane root ;
    @FXML
    private Rectangle border ;

    @FXML
    private MaskerPane masker_panel;

    
	@FXML
	MenuBar myMenuBar;

    @FXML
    private ToggleSwitch building_elements;

    @FXML
    private ToggleSwitch building_props;
	
	
    @FXML
    private RadioButton level1;
    @FXML
    private RadioButton level2;
    @FXML
    private RadioButton level3;

	
	@FXML
	private TextArea handleOnTxt;

	@FXML
	private Button selectIFCFileButton;

	@FXML
	private Button convert2RDFButton;
	@FXML
	private Label labelIFCFile;
	@FXML
	private TextArea conversionTxt;

	@FXML
	private ImageView owl_fileIcon;
	@FXML
	private ImageView rdf_fileIcon;

	@FXML
	private CustomTextField labelBaseURI;

	Image fileimage = new Image(getClass().getResourceAsStream("file.png"));

	FileChooser fc;

	@FXML
	private void closeApplicationAction() {
		// get a handle to the stage
		Stage stage = (Stage) myMenuBar.getScene().getWindow();
		stage.close();
	}

	@FXML
	private void aboutAction() {
		// get a handle to the stage
		Stage stage = (Stage) myMenuBar.getScene().getWindow();
		new About(stage).show();
	}

	final Tooltip openExpressFileButton_tooltip = new Tooltip();
	final Tooltip saveIfcOWLButton_tooltip = new Tooltip();
	private FxInterface application;

	private String ifcFileName = null;
	private String rdfTargetName = null;

	@FXML
	private void selectIFCFile() {
		Stage stage = (Stage) myMenuBar.getScene().getWindow();
		File file = null;

		if (fc == null) {
			fc = new FileChooser();
			fc.setInitialDirectory(new File("."));
		}
		FileChooser.ExtensionFilter ef1;
		ef1 = new FileChooser.ExtensionFilter("IFC documents (*.ifc)", "*.ifc");
		FileChooser.ExtensionFilter ef2;
		ef2 = new FileChooser.ExtensionFilter("All Files", "*.*");
		fc.getExtensionFilters().clear();
		fc.getExtensionFilters().addAll(ef1, ef2);

		if (file == null)
			file = fc.showOpenDialog(stage);
		if (file == null)
			return;
		fc.setInitialDirectory(file.getParentFile());
		labelIFCFile.setText(file.getName());
		ifcFileName = file.getAbsolutePath();
		int i = ifcFileName.lastIndexOf(".");
		if (i > 0) {
			rdfTargetName = ifcFileName.substring(0, i) + "_LBD.ttl";
		}
		if (ifcFileName != null && rdfTargetName != null) {
			convert2RDFButton.setDefaultButton(true);
			convert2RDFButton.setDisable(false);
		}
		selectIFCFileButton.setDefaultButton(false);
		rdf_fileIcon.setDisable(false);
		rdf_fileIcon.setImage(fileimage);
	}

	@FXML
	private void convertIFCToRDF() {
		conversionTxt.setText("");
		try {
			String uri_base = labelBaseURI.getText().trim();
			int props_level=2;
			if(level1.isSelected())
				props_level=1;
			if(level3.isSelected())
				props_level=3;
			masker_panel.setVisible(true);
			executor.submit(new ConversionThread(ifcFileName, uri_base, rdfTargetName,props_level,building_elements.isSelected(),building_props.isSelected()));
		} catch (Exception e) {
			Platform.runLater(() -> this.conversionTxt.appendText(e.getMessage()));
		}

	}

	public void initialize(URL location, ResourceBundle resources) {
		this.application = this;
		eventBus.register(this);
		border.widthProperty().bind(root.widthProperty());
        border.heightProperty().bind(root.heightProperty());
		// Accepts dropping
		EventHandler<DragEvent> ad_ontology = new EventHandler<DragEvent>() {
			public void handle(DragEvent event) {
				Dragboard db = event.getDragboard();
				if (db.hasFiles()) {
					event.acceptTransferModes(TransferMode.COPY);
				} else {
					event.consume();
				}
			}

		};

		// Accepts dropping
		EventHandler<DragEvent> ad_conversion = new EventHandler<DragEvent>() {
			public void handle(DragEvent event) {
				Dragboard db = event.getDragboard();
				if (db.hasFiles()) {
					event.acceptTransferModes(TransferMode.COPY);
				} else {
					event.consume();
				}
			}
		};

		// Dropping over surface
		EventHandler<DragEvent> dh_conversion = new EventHandler<DragEvent>() {
			public void handle(DragEvent event) {
				Dragboard db = event.getDragboard();
				boolean success = false;
				if (db.hasFiles()) {
					success = true;
					for (File file : db.getFiles()) {
						labelIFCFile.setText(file.getName());
						ifcFileName = file.getAbsolutePath();
						if (ifcFileName != null && rdfTargetName != null) {
							convert2RDFButton.setDefaultButton(true);
							selectIFCFileButton.setDefaultButton(false);
							convert2RDFButton.setDisable(false);
						}
						rdf_fileIcon.setDisable(false);
						rdf_fileIcon.setImage(fileimage);
					}
				}
				event.setDropCompleted(success);
				event.consume();
			}
		};

		selectIFCFileButton.setOnDragOver(ad_conversion);
		selectIFCFileButton.setOnDragDropped(dh_conversion);
		convert2RDFButton.setOnDragOver(ad_conversion);
		convert2RDFButton.setOnDragDropped(dh_conversion);
		labelIFCFile.setOnDragOver(ad_conversion);
		labelIFCFile.setOnDragDropped(dh_conversion);
		conversionTxt.setOnDragOver(ad_conversion);
		conversionTxt.setOnDragDropped(dh_conversion);

		rdf_fileIcon.setOnDragDetected(new EventHandler<MouseEvent>() {
			public void handle(MouseEvent me) {

				if (!rdf_fileIcon.isDisabled()) {
					Dragboard db = handleOnTxt.startDragAndDrop(TransferMode.ANY);

					ClipboardContent content = new ClipboardContent();
					Clipboard clipboard = Clipboard.getSystemClipboard();
					try {
						File temp = File.createTempFile("rdf", ".ttl");

						conversionTxt.setText("");
						try {
							String uri_base = labelBaseURI.getText().trim();
							int props_level=2;
							if(level1.isSelected())
								props_level=1;
							if(level3.isSelected())
								props_level=3;
							masker_panel.setVisible(true);
							executor.submit(new ConversionThread(ifcFileName, uri_base, temp.getAbsolutePath(),props_level,building_elements.isSelected(),building_props.isSelected()));
						} catch (Exception e) {
							conversionTxt.appendText(e.getMessage());
						}

						content.putFiles(java.util.Collections.singletonList(temp));
						db.setContent(content);
						clipboard.setContent(content);
					} catch (IOException e) {

						e.printStackTrace();
					}
				}
				me.consume();
			}
		});

		rdf_fileIcon.setOnDragDone(new EventHandler<DragEvent>() {
			public void handle(DragEvent me) {
				me.consume();
			}
		});
		this.labelBaseURI.setText("https://www.ugent.be/myAwesomeFirstBIMProject#");
	}

	public void handle_notification(String txt) {
		conversionTxt.insertText(0, txt + "\n");
	}

	@Subscribe
	public void handleEvent(final SystemStatusEvent event) {
		System.out.println("message: " + event.getStatus_message());
		Platform.runLater(() -> this.conversionTxt.appendText(event.getStatus_message() + "\n"));
	}
	
	@Subscribe
	public void handleEvent(final ProcessReadyEvent event) {
		this.masker_panel.setVisible(false);
	}
}

package com.project.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.project.dao.ProjektDAO;
import com.project.model.Projekt;
import javafx.scene.control.ButtonBar;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.GridPane;
import javafx.util.Callback;
import javafx.util.StringConverter;

public class ProjectController {
    private static final Logger logger = LoggerFactory.getLogger(ProjectController.class);

    private ExecutorService wykonawca;
    private ProjektDAO projektDAO;
    private ObservableList<Projekt> projekty;

    private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter dateTimeFormater = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // Zmienne do obsługi stronicowania i wyszukiwania
    private String search4;
    private Integer pageNo;
    private Integer pageSize;

    // Automatycznie wstrzykiwane komponenty GUI
    @FXML
    private ChoiceBox<Integer> cbPageSizes;
    @FXML
    private TableView<Projekt> tblProjekt;
    @FXML
    private TableColumn<Projekt, Integer> colId;
    @FXML
    private TableColumn<Projekt, String> colNazwa;
    @FXML
    private TableColumn<Projekt, String> colOpis;
    @FXML
    private TableColumn<Projekt, LocalDateTime> colDataCzasUtworzenia;
    @FXML
    private TableColumn<Projekt, LocalDate> colDataOddania;

    @FXML
    private TextField txtSzukaj;
    @FXML
    private Button btnDalej;
    @FXML
    private Button btnWstecz;
    @FXML
    private Button btnPierwsza;
    @FXML
    private Button btnOstatnia;

    public ProjectController(ProjektDAO projektDAO) {
        this.projektDAO = projektDAO;
        wykonawca = Executors.newFixedThreadPool(1); // Wystarczy jeden wątek do pobierania danych
    }

    public ProjectController() {
        // Domyślny konstruktor
    }

    @FXML
    public void initialize() {
        search4 = "";
        pageNo = 0;
        pageSize = 10;

        cbPageSizes.getItems().addAll(5, 10, 20, 50, 100);
        cbPageSizes.setValue(pageSize);

        colId.setCellValueFactory(new PropertyValueFactory<>("projektId"));
        colNazwa.setCellValueFactory(new PropertyValueFactory<>("nazwa"));
        colOpis.setCellValueFactory(new PropertyValueFactory<>("opis"));
        colDataCzasUtworzenia.setCellValueFactory(new PropertyValueFactory<>("dataCzasUtworzenia"));
        colDataOddania.setCellValueFactory(new PropertyValueFactory<>("dataOddania"));

        projekty = FXCollections.observableArrayList();
        tblProjekt.setItems(projekty);

        wykonawca.execute(() -> loadPage(search4, pageNo, pageSize));

        // Dodanie kolumny z przyciskami
        TableColumn<Projekt, Void> colEdit = new TableColumn<>("Akcje");
        colEdit.setCellFactory(column -> new TableCell<>() {
            private final Button btnEdit = new Button("Edytuj");
            private final Button btnRemove = new Button("Usuń");
            private final Button btnTask = new Button("Zadania");
            private final GridPane pane = new GridPane();

            {
                pane.setHgap(5);
                pane.add(btnEdit, 0, 0);
                pane.add(btnRemove, 1, 0);
                pane.add(btnTask, 2, 0);

                btnEdit.setOnAction(event -> edytujProjekt(getCurrentProjekt()));
                btnRemove.setOnAction(event -> usunProjekt(getCurrentProjekt()));
                btnTask.setOnAction(event -> openZadanieFrame(getCurrentProjekt()));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getCurrentProjekt() == null) {
                    setGraphic(null);
                } else {
                    setGraphic(pane);
                }
            }

            private Projekt getCurrentProjekt() {
                return getTableView().getItems().get(getIndex());
            }
        });

        tblProjekt.getColumns().add(colEdit);
    }

    private void edytujProjekt(Projekt projekt) {
        Dialog<Projekt> dialog = new Dialog<>();
        dialog.setTitle("Edycja");
        if (projekt.getProjektId() != null) {
            dialog.setHeaderText("Edycja danych projektu");
        } else {
            dialog.setHeaderText("Dodawanie projektu");
        }
        dialog.setResizable(true);

        Label lblId = getRightLabel("Id: ");
        Label lblNazwa = getRightLabel("Nazwa: ");
        Label lblOpis = getRightLabel("Opis: ");
        Label lblDataCzasUtworzenia = getRightLabel("Data utworzenia: ");
        Label lblDataOddania = getRightLabel("Data oddania: ");

        Label txtId = new Label();
        if (projekt.getProjektId() != null)
            txtId.setText(projekt.getProjektId().toString());

        TextField txtNazwa = new TextField();
        if (projekt.getNazwa() != null)
            txtNazwa.setText(projekt.getNazwa());

        TextArea txtOpis = new TextArea();
        txtOpis.setPrefRowCount(6);
        txtOpis.setPrefColumnCount(40);
        txtOpis.setWrapText(true);
        if (projekt.getOpis() != null)
            txtOpis.setText(projekt.getOpis());

        Label txtDataUtworzenia = new Label();
        if (projekt.getDataCzasUtworzenia() != null)
            txtDataUtworzenia.setText(dateTimeFormater.format(projekt.getDataCzasUtworzenia()));

        DatePicker dtDataOddania = new DatePicker();
        dtDataOddania.setPromptText("RRRR-MM-DD");
        dtDataOddania.setConverter(new StringConverter<>() {
            @Override
            public String toString(LocalDate date) {
                return date != null ? dateFormatter.format(date) : null;
            }

            @Override
            public LocalDate fromString(String text) {
                return text == null || text.trim().isEmpty() ? null : LocalDate.parse(text, dateFormatter);
            }
        });

        dtDataOddania.getEditor().focusedProperty().addListener((obsValue, oldFocus, newFocus) -> {
            if (!newFocus) {
                try {
                    dtDataOddania.setValue(dtDataOddania.getConverter().fromString(
                            dtDataOddania.getEditor().getText()));
                } catch (DateTimeParseException e) {
                    dtDataOddania.getEditor().setText(dtDataOddania.getConverter()
                            .toString(dtDataOddania.getValue()));
                }
            }
        });

        if (projekt.getDataOddania() != null) {
            dtDataOddania.setValue(projekt.getDataOddania());
        }

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(5, 5, 5, 5));
        grid.add(lblId, 0, 0);
        grid.add(txtId, 1, 0);
        grid.add(lblDataCzasUtworzenia, 0, 1);
        grid.add(txtDataUtworzenia, 1, 1);
        grid.add(lblNazwa, 0, 2);
        grid.add(txtNazwa, 1, 2);
        grid.add(lblOpis, 0, 3);
        grid.add(txtOpis, 1, 3);
        grid.add(lblDataOddania, 0, 4);
        grid.add(dtDataOddania, 1, 4);

        dialog.getDialogPane().setContent(grid);

        ButtonType buttonTypeOk = new ButtonType("Zapisz", ButtonBar.ButtonData.OK_DONE);
        ButtonType buttonTypeCancel = new ButtonType("Anuluj", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(buttonTypeOk, buttonTypeCancel);

        dialog.setResultConverter(buttonType -> {
            if (buttonType == buttonTypeOk) {
                projekt.setNazwa(txtNazwa.getText().trim());
                projekt.setOpis(txtOpis.getText().trim());
                projekt.setDataOddania(dtDataOddania.getValue());
                return projekt;
            }
            return null;
        });

        Optional<Projekt> result = dialog.showAndWait();
        if (result.isPresent()) {
            wykonawca.execute(() -> {
                try {
                    projektDAO.setProjekt(projekt);
                    Platform.runLater(() -> {
                        if (tblProjekt.getItems().contains(projekt)) {
                            tblProjekt.refresh();
                        } else {
                            tblProjekt.getItems().add(0, projekt);
                        }
                    });
                } catch (RuntimeException e) {
                    String errMsg = "Błąd podczas zapisywania danych projektu!";
                    logger.error(errMsg, e);
                    Platform.runLater(() -> showError(errMsg, e.getMessage()));
                }
            });
        }
    }

    private Label getRightLabel(String text) {
        Label lbl = new Label(text);
        lbl.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        lbl.setAlignment(Pos.CENTER_RIGHT);
        return lbl;
    }
    
    private void loadPage(String search4, Integer pageNo, Integer pageSize) {
        try {
            final List<Projekt> projektList = new ArrayList<>();
            if (search4 != null && !search4.isEmpty()) {
                if (search4.matches("[0-9]+")) {
                    Projekt p = projektDAO.getProjekt(Integer.valueOf(search4));
                    if (p != null) projektList.add(p);
                } else if (search4.matches("^[0-9]{4}-(0[1-9]|1[0-2])-(0[1-9]|[12][0-9]|3[01])$")) {
                    projektList.addAll(projektDAO.getProjektyWhereDataOddaniaIs(LocalDate.parse(search4), pageNo * pageSize, pageSize));
                } else {
                    projektList.addAll(projektDAO.getProjektyWhereNazwaLike(search4, pageNo * pageSize, pageSize));
                }
            } else {
                projektList.addAll(projektDAO.getProjekty(pageNo * pageSize, pageSize));
            }

            Platform.runLater(() -> {
                projekty.clear();
                projekty.addAll(projektList);
            });
        } catch (RuntimeException e) {
            String errMsg = "Błąd podczas pobierania listy projektów.";
            logger.error(errMsg, e);
            Platform.runLater(() -> showError(errMsg, e.getMessage()));
        }
    }

    private void usunProjekt(Projekt projekt) {
        Alert alert = new Alert(AlertType.CONFIRMATION);
        alert.setTitle("Usuwanie projektu");
        alert.setHeaderText("Czy na pewno chcesz usunąć projekt?");
        alert.setContentText("Projekt: " + projekt.getNazwa());
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            wykonawca.execute(() -> {
                try {
                    projektDAO.deleteProjekt(projekt.getProjektId());
                    Platform.runLater(() -> projekty.remove(projekt));
                } catch (RuntimeException e) {
                    String errMsg = "Błąd podczas usuwania projektu!";
                    logger.error(errMsg, e);
                    Platform.runLater(() -> showError(errMsg, e.getMessage()));
                }
            });
        }
    }

    private void openZadanieFrame(Projekt projekt) {
        // TODO: Implementacja otwierania okna zadań dla projektu
        logger.info("Otwieranie okna zadań dla projektu: {}", projekt.getNazwa());
    }

    private void showError(String header, String content) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle("Błąd");
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    public void shutdown() {
        if (wykonawca != null) {
            wykonawca.shutdown();
            try {
                if (!wykonawca.awaitTermination(5, TimeUnit.SECONDS)) {
                    wykonawca.shutdownNow();
                }
            } catch (InterruptedException e) {
                wykonawca.shutdownNow();
            }
        }
    }

    @FXML
    private void onActionBtnSzukaj(ActionEvent event) {
        search4 = txtSzukaj.getText().trim();
        pageNo = 0;
        wykonawca.execute(() -> loadPage(search4, pageNo, pageSize));
    }

    @FXML
    private void onActionBtnDalej(ActionEvent event) {
        pageNo++;
        wykonawca.execute(() -> loadPage(search4, pageNo, pageSize));
    }

    @FXML
    private void onActionBtnWstecz(ActionEvent event) {
        if (pageNo > 0) {
            pageNo--;
            wykonawca.execute(() -> loadPage(search4, pageNo, pageSize));
        }
    }

    @FXML
    private void onActionBtnPierwsza(ActionEvent event) {
        pageNo = 0;
        wykonawca.execute(() -> loadPage(search4, pageNo, pageSize));
    }

    @FXML
    private void onActionBtnOstatnia(ActionEvent event) {
        int rows = projektDAO.getRowsNumber();
        pageNo = (rows - 1) / pageSize;
        wykonawca.execute(() -> loadPage(search4, pageNo, pageSize));
    }
}
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
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.util.StringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.project.dao.ProjektDAO;
import com.project.dao.ProjektDAOImpl;
import com.project.model.Projekt;

public class ProjectController {
    private static final Logger logger = LoggerFactory.getLogger(ProjectController.class);
    private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private String search4;
    private Integer pageNo;
    private Integer pageSize;
    private ObservableList<Projekt> projekty;
    private ExecutorService wykonawca;
    private ProjektDAO projektDAO;

    @FXML private ChoiceBox<Integer> cbPageSizes;
    @FXML private TableView<Projekt> tblProjekt;
    @FXML private TableColumn<Projekt, Integer> colId;
    @FXML private TableColumn<Projekt, String> colNazwa;
    @FXML private TableColumn<Projekt, String> colOpis;
    @FXML private TableColumn<Projekt, LocalDateTime> colDataCzasUtworzenia;
    @FXML private TableColumn<Projekt, LocalDate> colDataOddania;
    @FXML private TextField txtSzukaj;
    @FXML private Button btnDalej;
    @FXML private Button btnWstecz;
    @FXML private Button btnPierwsza;
    @FXML private Button btnOstatnia;
    @FXML private Label lblPageInfo;

    public ProjectController() {
        this.projektDAO = new ProjektDAOImpl();
        this.wykonawca = Executors.newFixedThreadPool(2); // Zwiększamy pulę wątków na 2
    }

    public ProjectController(ProjektDAO projektDAO) {
        this.projektDAO = projektDAO;
        this.wykonawca = Executors.newFixedThreadPool(2);
    }

    @FXML
    public void initialize() {
        search4 = "";
        pageNo = 0;
        pageSize = 10;

        cbPageSizes.getItems().addAll(5, 10, 20, 50, 100);
        cbPageSizes.setValue(pageSize);
        cbPageSizes.setOnAction(event -> {
            pageSize = cbPageSizes.getValue();
            pageNo = 0;
            logger.info("Zmiana pageSize na: {}, pageNo: {}", pageSize, pageNo);
            wykonawca.execute(() -> loadPage(search4, pageNo, pageSize));
        });

        colId.setCellValueFactory(new PropertyValueFactory<>("projektId"));
        colNazwa.setCellValueFactory(new PropertyValueFactory<>("nazwa"));
        colOpis.setCellValueFactory(new PropertyValueFactory<>("opis"));
        colDataCzasUtworzenia.setCellValueFactory(new PropertyValueFactory<>("dataCzasUtworzenia"));
        colDataOddania.setCellValueFactory(new PropertyValueFactory<>("dataOddania"));

        colDataCzasUtworzenia.setCellFactory(column -> new TableCell<Projekt, LocalDateTime>() {
            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                setText(item == null || empty ? null : dateTimeFormatter.format(item));
            }
        });

        colDataOddania.setCellFactory(column -> new TableCell<Projekt, LocalDate>() {
            @Override
            protected void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                setText(item == null || empty ? null : dateFormatter.format(item));
            }
        });

        TableColumn<Projekt, Void> colEdit = new TableColumn<>("Edycja");
        colEdit.setCellFactory(column -> new TableCell<Projekt, Void>() {
            private final GridPane pane;
            {
                Button btnEdit = new Button("Edytuj");
                Button btnRemove = new Button("Usuń");
                Button btnTask = new Button("Zadania");
                btnEdit.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
                btnRemove.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
                btnTask.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

                btnEdit.setOnAction(event -> edytujProjekt(getCurrentProjekt()));
                btnRemove.setOnAction(event -> usunProjekt(getCurrentProjekt()));
                btnTask.setOnAction(event -> {
                    // TODO: openZadanieFrame(getCurrentProjekt());
                });

                pane = new GridPane();
                pane.setAlignment(Pos.CENTER);
                pane.setHgap(10);
                pane.setVgap(10);
                pane.setPadding(new Insets(5, 5, 5, 5));
                pane.add(btnTask, 0, 0);
                pane.add(btnEdit, 0, 1);
                pane.add(btnRemove, 0, 2);
            }

            private Projekt getCurrentProjekt() {
                int index = this.getTableRow().getIndex();
                return this.getTableView().getItems().get(index);
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
            }
        });

        tblProjekt.getColumns().add(colEdit);

        colId.setMaxWidth(5000);
        colNazwa.setMaxWidth(10000);
        colOpis.setMaxWidth(10000);
        colDataCzasUtworzenia.setMaxWidth(9000);
        colDataOddania.setMaxWidth(7000);
        colEdit.setMaxWidth(7000);

        projekty = FXCollections.observableArrayList();
        tblProjekt.setItems(projekty);

        logger.info("Inicjalizacja: pageNo: {}, pageSize: {}", pageNo, pageSize);
        wykonawca.execute(() -> loadPage(search4, pageNo, pageSize));
    }

    @FXML
    private void onActionBtnSzukaj(ActionEvent event) {
        search4 = txtSzukaj.getText().trim();
        pageNo = 0;
        logger.info("Wyszukiwanie: search4: {}, pageNo: {}", search4, pageNo);
        wykonawca.execute(() -> loadPage(search4, pageNo, pageSize));
    }

    @FXML
    private void onActionBtnDalej(ActionEvent event) {
        int totalRows = search4.isEmpty() ? projektDAO.getRowsNumber() : projektDAO.getRowsNumberWhereNazwaLike(search4);
        int totalPages = (int) Math.ceil((double) totalRows / pageSize);
        if (pageNo < totalPages - 1) {
            pageNo++;
            logger.info("Dalej: pageNo: {}, totalPages: {}", pageNo, totalPages);
            wykonawca.execute(() -> loadPage(search4, pageNo, pageSize));
        } else {
            logger.info("Dalej: brak kolejnych stron, pageNo: {}, totalPages: {}", pageNo, totalPages);
        }
    }

    @FXML
    private void onActionBtnWstecz(ActionEvent event) {
        logger.info("Wstecz kliknięty: pageNo przed zmianą: {}", pageNo);
        if (pageNo > 0) {
            pageNo--;
            logger.info("Wstecz: pageNo po zmianie: {}", pageNo);
            wykonawca.execute(() -> {
                logger.info("Wykonawca: Rozpoczęcie loadPage dla pageNo: {}", pageNo);
                loadPage(search4, pageNo, pageSize);
            });
        } else {
            logger.info("Wstecz: już na pierwszej stronie, pageNo: {}", pageNo);
        }
    }

    @FXML
    private void onActionBtnPierwsza(ActionEvent event) {
        pageNo = 0;
        logger.info("Pierwsza: pageNo: {}", pageNo);
        wykonawca.execute(() -> loadPage(search4, pageNo, pageSize));
    }

    @FXML
    private void onActionBtnOstatnia(ActionEvent event) {
        int totalRows = search4.isEmpty() ? projektDAO.getRowsNumber() : projektDAO.getRowsNumberWhereNazwaLike(search4);
        int totalPages = (int) Math.ceil((double) totalRows / pageSize);
        pageNo = totalPages - 1;
        logger.info("Ostatnia: pageNo: {}, totalPages: {}", pageNo, totalPages);
        wykonawca.execute(() -> loadPage(search4, pageNo, pageSize));
    }

    @FXML
    private void onActionBtnDodaj(ActionEvent event) {
        edytujProjekt(new Projekt());
    }

    private void loadPage(String search4, Integer pageNo, Integer pageSize) {
        try {
            int offset = pageNo * pageSize;
            logger.info("Ładowanie strony: search4: {}, pageNo: {}, pageSize: {}, offset: {}", search4, pageNo, pageSize, offset);
            final List<Projekt> projektList = new ArrayList<>();
            if (search4 != null && !search4.isEmpty()) {
                if (search4.matches("[0-9]+")) {	// jesli same cyfry
                    Projekt projekt = projektDAO.getProjekt(Integer.parseInt(search4));
                    if (projekt != null) projektList.add(projekt);
                    projektList.addAll(projektDAO.getProjektyWhereNazwaLike(search4, offset, pageSize));	// jesli nazwa to cyfry
                    logger.info("Wyszukano po ID: {}", projekt != null ? projekt.getProjektId() : "brak");
                } else if (search4.matches("^[0-9]{4}-(0[1-9]|1[0-2])-(0[1-9]|[12][0-9]|3[01])$")) {
                    projektList.addAll(projektDAO.getProjektyWhereDataOddaniaIs(LocalDate.parse(search4, dateFormatter), offset, pageSize));
                    logger.info("Wyszukano po dacie: {} rekordów", projektList.size());
                } else {
                    projektList.addAll(projektDAO.getProjektyWhereNazwaLike(search4, offset, pageSize));
                    logger.info("Wyszukano po nazwie: {} rekordów", projektList.size());
                }
            } else {
                projektList.addAll(projektDAO.getProjekty(offset, pageSize));
                logger.info("Wczytano wszystkie: {} rekordów", projektList.size());
            }
            Platform.runLater(() -> {
                logger.info("Platform.runLater: Aktualizacja tabeli, rekordów: {}", projektList.size());
                projekty.clear();
                projekty.addAll(projektList);
                updatePaginationButtons();
                logger.info("Załadowano do tabeli: {} rekordów na stronie {}", projekty.size(), pageNo);
            });
        } catch (RuntimeException e) {
            String errMsg = "Błąd podczas pobierania listy projektów.";
            logger.error(errMsg, e);
            String errDetails = e.getCause() != null ? e.getMessage() + "\n" + e.getCause().getMessage() : e.getMessage();
            Platform.runLater(() -> showError(errMsg, errDetails));
        }
    }

    private void updatePaginationButtons() {
        int totalRows = search4.isEmpty() ? projektDAO.getRowsNumber() : projektDAO.getRowsNumberWhereNazwaLike(search4);
        int totalPages = (int) Math.ceil((double) totalRows / pageSize);
        btnDalej.setDisable(pageNo >= totalPages - 1);
        btnWstecz.setDisable(pageNo <= 0);
        btnPierwsza.setDisable(pageNo <= 0);
        btnOstatnia.setDisable(totalPages <= 1 || pageNo >= totalPages - 1);

        String pageText = totalRows > 0 ? "Strona " + (pageNo + 1) + " z " + totalPages : "Brak rekordów";
        lblPageInfo.setText(pageText);

        logger.info("Aktualizacja przycisków: totalRows: {}, totalPages: {}, pageNo: {}, btnDalej: {}, btnWstecz: {}, lblPageInfo: {}", 
                    totalRows, totalPages, pageNo, btnDalej.isDisable(), btnWstecz.isDisable(), pageText);
    }

    private void edytujProjekt(Projekt projekt) {
        Dialog<Projekt> dialog = new Dialog<>();
        dialog.setTitle("Edycja");
        dialog.setHeaderText(projekt.getProjektId() != null ? "Edycja danych projektu" : "Dodawanie projektu");
        dialog.setResizable(true);

        Label lblId = getRightLabel("Id: ");
        Label lblNazwa = getRightLabel("Nazwa: ");
        Label lblOpis = getRightLabel("Opis: ");
        Label lblDataCzasUtworzenia = getRightLabel("Data utworzenia: ");
        Label lblDataOddania = getRightLabel("Data oddania: ");

        Label txtId = new Label(projekt.getProjektId() != null ? projekt.getProjektId().toString() : "");
        TextField txtNazwa = new TextField(projekt.getNazwa() != null ? projekt.getNazwa() : "");
        TextArea txtOpis = new TextArea(projekt.getOpis() != null ? projekt.getOpis() : "");
        txtOpis.setPrefRowCount(6);
        txtOpis.setPrefColumnCount(40);
        txtOpis.setWrapText(true);
        Label txtDataUtworzenia = new Label(projekt.getDataCzasUtworzenia() != null ? dateTimeFormatter.format(projekt.getDataCzasUtworzenia()) : "");

        DatePicker dtDataOddania = new DatePicker();
        dtDataOddania.setPromptText("RRRR-MM-DD");
        dtDataOddania.setConverter(new StringConverter<LocalDate>() {
            @Override
            public String toString(LocalDate date) {
                return date != null ? dateFormatter.format(date) : "";
            }

            @Override
            public LocalDate fromString(String text) {
                return text == null || text.trim().isEmpty() ? null : LocalDate.parse(text, dateFormatter);
            }
        });
        dtDataOddania.getEditor().focusedProperty().addListener((obs, oldFocus, newFocus) -> {
            if (!newFocus) {
                try {
                    dtDataOddania.setValue(dtDataOddania.getConverter().fromString(dtDataOddania.getEditor().getText()));
                } catch (DateTimeParseException e) {
                    dtDataOddania.getEditor().setText(dtDataOddania.getConverter().toString(dtDataOddania.getValue()));
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
        ButtonType buttonTypeOk = new ButtonType("Zapisz", ButtonData.OK_DONE);
        ButtonType buttonTypeCancel = new ButtonType("Anuluj", ButtonData.CANCEL_CLOSE);
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
                        loadPage(search4, pageNo, pageSize);
                    });
                } catch (RuntimeException e) {
                    String errMsg = "Błąd podczas zapisywania danych projektu!";
                    logger.error(errMsg, e);
                    String errDetails = e.getCause() != null ? e.getMessage() + "\n" + e.getCause().getMessage() : e.getMessage();
                    Platform.runLater(() -> showError(errMsg, errDetails));
                }
            });
        }
    }

    private void usunProjekt(Projekt projekt) {
        Alert confirmation = new Alert(AlertType.CONFIRMATION);
        confirmation.setTitle("Potwierdzenie");
        confirmation.setHeaderText("Usuwanie projektu");
        confirmation.setContentText("Czy na pewno chcesz usunąć projekt: " + projekt.getNazwa() + "o ID = "+ projekt.getProjektId() + "?");

        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            wykonawca.execute(() -> {
                try {
                    projektDAO.deleteProjekt(projekt.getProjektId());
                    Platform.runLater(() -> {
                        loadPage(search4, pageNo, pageSize);
                    });
                } catch (RuntimeException e) {
                    String errMsg = "Błąd podczas usuwania projektu!";
                    logger.error(errMsg, e);
                    String errDetails = e.getCause() != null ? e.getMessage() + "\n" + e.getCause().getMessage() : e.getMessage();
                    Platform.runLater(() -> showError(errMsg, errDetails));
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
}
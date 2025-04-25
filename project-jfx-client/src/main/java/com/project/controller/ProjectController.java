package com.project.controller;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.project.model.Projekt;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.collections.ObservableList;
import javafx.collections.FXCollections;
import javafx.application.Platform;
import com.project.dao.ProjektDAO;
import com.project.dao.ProjektDAOImpl;
import com.project.model.Projekt;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ProjectController {
	private ExecutorService wykonawca;
	private ProjektDAO projektDAO;
	 private ObservableList<Projekt> projekty;
 private static final Logger logger = LoggerFactory.getLogger(ProjectController.class);
 private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
 private static final DateTimeFormatter dateTimeFormater = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
 //Zmienne do obsługi stronicowania i wyszukiwania
 private String search4;
 private Integer pageNo;
 private Integer pageSize;

 //Automatycznie wstrzykiwane komponenty GUI
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
 wykonawca = Executors.newFixedThreadPool(1);// W naszej aplikacji wystarczy jeden wątek do pobierania
// danych. Przekazanie większej ilości takich zadań do puli
 } 
 
 public ProjectController() { //Utworzeniu konstruktora jest obligatoryjne
 }
 @FXML
 public void initialize() { //Metoda automatycznie wywoływana przez JavaFX zaraz po wstrzyknięciu
 search4 = ""; //wszystkich komponentów. Uwaga! Wszelkie modyfikacje komponentów
 pageNo = 0; //(np. cbPageSizes) trzeba realizować wewnątrz tej metody. Nigdy
 pageSize = 10; //nie używaj do tego celu konstruktora.
 cbPageSizes.getItems().addAll(5, 10, 20, 50, 100);
 cbPageSizes.setValue(pageSize);
 colId.setCellValueFactory(new PropertyValueFactory<Projekt, Integer>("projektId"));
 colNazwa.setCellValueFactory(new PropertyValueFactory<Projekt, String>("nazwa"));
 colOpis.setCellValueFactory(new PropertyValueFactory<Projekt, String>("opis"));
 colDataCzasUtworzenia.setCellValueFactory(new PropertyValueFactory<Projekt, LocalDateTime>
 ("dataCzasUtworzenia"));
 
 colDataOddania.setCellValueFactory(new PropertyValueFactory<Projekt, LocalDate>("dataOddania"));

 projekty = FXCollections.observableArrayList();

 //Powiązanie tabeli z listą typu ObservableList przechowującą projekty
 tblProjekt.setItems(projekty);
 wykonawca.execute(() -> loadPage(search4, pageNo, pageSize));

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

 projektList.addAll(projektDAO.getProjektyWhereNazwaLike(search4, pageNo * pageSize, pageSize));
 }else {
 projektList.addAll(projektDAO.getProjekty(pageNo * pageSize, pageSize));
 }
 Platform.runLater(() -> {
 projekty.clear();
 projekty.addAll(projektList);
 });
 } catch (RuntimeException e) {
 String errMsg = "Błąd podczas pobierania listy projektów.";
 logger.error(errMsg, e);
 String errDetails = e.getCause() != null ?
 e.getMessage() + "\n" + e.getCause().getMessage()
 : e.getMessage();
 Platform.runLater(() -> showError(errMsg, errDetails));
 }
 }

 /** Metoda pomocnicza do prezentowania użytkownikowi informacji o błędach */
 private void showError(String header, String content) {
 Alert alert = new Alert(AlertType.ERROR);
 alert.setTitle("Błąd");
 alert.setHeaderText(header);
 alert.setContentText(content);
 alert.showAndWait();
 }
 public void shutdown() {
 // Wystarczyłoby tylko samo wywołanie metody wykonawca.shutdownNow(), ale można również, tak jak poniżej,
 // zaimplementować wersję z oczekiwaniem na zakończenie wszystkich zadań wykonywanych w puli wątków.
 if(wykonawca != null) {
 wykonawca.shutdown();
 try {
 if(!wykonawca.awaitTermination(5, TimeUnit.SECONDS))
 wykonawca.shutdownNow();
 } catch (InterruptedException e) {
 wykonawca.shutdownNow();
 }
 }
 }

 
 //Grupa metod do obsługi przycisków
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
     int rows = 0;
     if (search4 != null && !search4.isEmpty()) {
         if (search4.matches("[0-9]+")) {
             rows = 1;
         } else if (search4.matches("^[0-9]{4}-(0[1-9]|1[0-2])-(0[1-9]|[12][0-9]|3[01])$")) {
             rows = projektDAO.getRowsNumberWhereDataOddaniaIs(LocalDate.parse(search4));
         } else {
             rows = projektDAO.getRowsNumberWhereNazwaLike(search4);
         }
     } else {
         rows = projektDAO.getRowsNumber();
     }
     pageNo = (rows - 1) / pageSize;
     wykonawca.execute(() -> loadPage(search4, pageNo, pageSize));
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
                 String errDetails = e.getCause() != null
                         ? e.getMessage() + "\n" + e.getCause().getMessage()
                         : e.getMessage();
                 Platform.runLater(() -> showError(errMsg, errDetails));
             }
         });
     }
 }
}
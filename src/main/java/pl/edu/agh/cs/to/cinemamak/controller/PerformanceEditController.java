package pl.edu.agh.cs.to.cinemamak.controller;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import net.rgielen.fxweaver.core.FxmlView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import pl.edu.agh.cs.to.cinemamak.event.TablePerformanceChangeEvent;
import pl.edu.agh.cs.to.cinemamak.model.Movie;
import pl.edu.agh.cs.to.cinemamak.model.Performance;
import pl.edu.agh.cs.to.cinemamak.model.Room;
import pl.edu.agh.cs.to.cinemamak.model.User;
import pl.edu.agh.cs.to.cinemamak.service.MovieService;
import pl.edu.agh.cs.to.cinemamak.service.PerformanceService;
import pl.edu.agh.cs.to.cinemamak.service.RoomService;
import pl.edu.agh.cs.to.cinemamak.service.UserService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
@FxmlView("performance-edit-view.fxml")
public class PerformanceEditController {

    @FXML
    private ChoiceBox<String> movieChoiceBox;
    @FXML
    private DatePicker datePicker;
    @FXML
    private Spinner<String> hourSpinner;
    @FXML
    private ChoiceBox<String> roomChoiceBox;
    @FXML
    private ChoiceBox<String> supervisorChoiceBox;
    @FXML
    private TextField priceTextField;
    @FXML
    private Button applyButton;
    @FXML
    private Button cancelButton;

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    private final MovieService movieService;
    private final RoomService roomService;
    private final UserService userService;
    private final PerformanceService performanceService;
    private Stage stage;

    private Optional<Performance> performance = Optional.empty();

    public PerformanceEditController(MovieService movieService,
                                     RoomService roomService,
                                     UserService userService,
                                     PerformanceService performanceService){
        this.userService = userService;
        this.roomService = roomService;
        this.movieService = movieService;
        this.performanceService = performanceService;
    }

    public PerformanceEditController setStage(Stage stage) {
        this.stage = stage;
        return this;
    }

    public PerformanceEditController setPerformance(Performance perf) {
        this.performance = Optional.of(perf);
        return this;
    }

    public void initialize(){

        if(performance.isPresent()) {
            this.setFields();
        }
        this.userService.getUsers().ifPresent(list -> list.forEach(user ->
                this.supervisorChoiceBox.getItems().add(user.getId()+" "+user.getFirstName()+ " " + user.getLastName())));

        this.roomService.getRooms().ifPresent(list -> list.forEach(room ->
                this.roomChoiceBox.getItems().add(room.getId()+" "+room.getName())));

        this.movieService.getMovies().ifPresent(list -> list.forEach(movie ->
                this.movieChoiceBox.getItems().add(movie.getId()+" "+movie.getTitle())));

        List<String> hours = new ArrayList<>();
        for(int i = 8; i<24; i++){
            hours.add(i+":00");
            hours.add(i+":30");
        }

        SpinnerValueFactory<String> valueFactory = new SpinnerValueFactory<String>() {
            int ptr = 0;
            @Override
            public void decrement(int steps) {
                if(ptr - steps != -1) ptr -= steps;
                setValue(hours.get(ptr));
            }

            @Override
            public void increment(int steps) {
                if(ptr+ steps != hours.size()) ptr += steps;
                setValue(hours.get(ptr));
            }
        };

        this.hourSpinner.setValueFactory(valueFactory);
    }

    public void setFields(){
        if(this.performance.isPresent()) {
            Performance perf = this.performance.get();
            System.out.println(perf);
            this.supervisorChoiceBox.setValue("value2");
            String hourStr;
            if (perf.getDate().getMinute() == 0) {
                hourStr = perf.getDate().getHour() + ":00";
            } else {
                hourStr = perf.getDate().getHour() + ":" + perf.getDate().getMinute();
            }
            this.hourSpinner.setPromptText(hourStr);
            this.priceTextField.setText(perf.getPrice().toString());
            this.datePicker.setValue(perf.getDate().toLocalDate());
            this.movieChoiceBox.setValue(perf.getMovie().getId() + " " + perf.getMovie().getTitle());
            this.roomChoiceBox.setValue(perf.getRoom().getId() + " " + perf.getRoom().getName());
            this.supervisorChoiceBox.setValue(perf.getUser().getId() + " " + perf.getUser().getFirstName() + " " + perf.getUser().getLastName());
        }
    }

    public void onActionApply(){
        String title = this.movieChoiceBox.getValue();
        String name_room = this.roomChoiceBox.getValue();
        String supervisor = this.supervisorChoiceBox.getValue();
        Double price = null;

        try {
            price = Double.parseDouble(
                    this.priceTextField.getCharacters().toString());
        } catch (NullPointerException nullPointerException){
            showErrorDialog("Error occurred while editing performance",
                    "All fields need to be filled!");
            return;
        } catch(NumberFormatException numberFormatException){
            showErrorDialog("Error occurred while editing performance",
                    "Price need to be in format: integer.integer or integer.");
            return;
        }

        LocalDate date = this.datePicker.getValue();
        String hour_str = this.hourSpinner.getValue();

        if(hour_str != null && date != null && title != null && name_room  != null && supervisor  != null ){
            int hour1 = Integer.parseInt(hour_str.split(":")[0]);
            int minute1 = Integer.parseInt(hour_str.split(":")[1]);
            LocalTime time = LocalTime.of(hour1, minute1, 0);
            Optional<Movie> movie = movieService.getMovieById(Long.parseLong(title.split("\\s")[0]));
            Optional<Room> room = roomService.getRoomById(Long.parseLong(name_room.split("\\s")[0]));
            Optional<User> user = userService.getUserById(Long.parseLong(supervisor.split("\\s")[0]));

            LocalDateTime localDateTime1 = LocalDateTime.of(date, time);

            if(movie.isPresent() && room.isPresent() && user.isPresent() && this.performance.isPresent()){
                this.performance.get().setUser(user.get());
                this.performance.get().setMovie(movie.get());
                this.performance.get().setRoom(room.get());
                this.performance.get().setPrice(BigDecimal.valueOf(price));
                this.performance.get().setDate(localDateTime1);

                this.performanceService.addPerformance(this.performance.get());

                Alert dialog = new Alert(Alert.AlertType.INFORMATION);
                dialog.initModality(Modality.APPLICATION_MODAL);
                dialog.initOwner(stage);
                dialog.setTitle("Information");
                dialog.setHeaderText("Performance edited successfully");
                dialog.show();
                dialog.setOnCloseRequest(event -> {
                    applicationEventPublisher.publishEvent(new TablePerformanceChangeEvent(this));
                    stage.close();
                });

            }
            else{
                showErrorDialog("Error occurred while editing performance",
                        "All fields need to be filled!");
            }
        }
        else{
            showErrorDialog("Error occurred while editing performance",
                    "All fields need to be filled! (look at hour field)");
        }

    }

    public void showErrorDialog(String header, String info){
        Alert dialog = new Alert(Alert.AlertType.ERROR);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(stage);
        dialog.setTitle("Error");
        dialog.setHeaderText(header);
        dialog.setContentText(info);
        dialog.show();
    }

    public void onActionCancel(){
        stage.close();
    }


}

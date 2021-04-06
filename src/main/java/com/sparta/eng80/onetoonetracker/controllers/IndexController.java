package com.sparta.eng80.onetoonetracker.controllers;

import com.sparta.eng80.onetoonetracker.entities.*;
import com.sparta.eng80.onetoonetracker.entities.datatypes.Status;
import com.sparta.eng80.onetoonetracker.services.*;
import com.sparta.eng80.onetoonetracker.utilities.TrainerTraineeEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Controller
public class IndexController {

    private final SecurityService securityService;
    private final GroupService groupService;
    private final StreamService streamService;
    private final TrainerService trainerService;
    private final TraineeService traineeService;

    @Autowired
    public IndexController(SecurityService securityService, GroupService groupService, StreamService streamService, TrainerService trainerService, TraineeService traineeService) {
        this.securityService = securityService;
        this.groupService = groupService;
        this.streamService = streamService;
        this.trainerService = trainerService;
        this.traineeService = traineeService;
    }

    @GetMapping("/")
    public String method(ModelMap model) {
        if(securityService.isAuthenticated()){
            switch (securityService.getCurrentUser().getRole()) {
                case "ROLE_TRAINER":
                    TrainerEntity trainer = securityService.getCurrentUser().getTrainer();

                    model.addAttribute("trainer", trainer);
                    model.addAttribute("allUnassignedGroups", groupService.findAllUnassigned());
                    model.addAttribute("allStreams", streamService.findAll());
                    model.addAttribute("allTrainers", trainerService.findAll());
                    Iterable<TraineeEntity> trainees = traineeService.findByGroupId(trainer.getGroup().getGroupId());
                    Iterable<FeedbackEntity> feedbackSheets = groupService.findAllFeedbackFromGroup(trainer.getGroup().getGroupId());
                    ArrayList<TrainerTraineeEntity> feedbackSheetsInCorrectOrder = new ArrayList<>();
                    for (TraineeEntity traineeEntity : trainees) {
                        TrainerTraineeEntity trainerTraineeEntity = new TrainerTraineeEntity();
                        trainerTraineeEntity.setTraineeEntity(traineeEntity);
                        boolean found = false;
                        if (feedbackSheets.iterator().hasNext()) {
                            for (Object feedbackEntity : feedbackSheets) {
                                if (((FeedbackEntity) feedbackEntity).getTrainee().getTraineeId() == traineeEntity.getTraineeId()) {
                                    trainerTraineeEntity.setFeedbackEntity(((FeedbackEntity) feedbackEntity));
                                    found = true;
                                }
                            }
                        }
                        if (!found) {
                            FeedbackEntity feedbackEntity = new FeedbackEntity();
                            feedbackEntity.setStatus(Status.IN_PROGRESS);
                            trainerTraineeEntity.setFeedbackEntity(feedbackEntity);
                        }
                        feedbackSheetsInCorrectOrder.add(trainerTraineeEntity);
                    }
                    model.addAttribute("feedbackStatus", feedbackSheetsInCorrectOrder);
                    break;
                case "ROLE_ADMIN":
                    model.addAttribute("trainers", trainerService.findAll());
                    break;
                case "ROLE_TRAINEE":
                    TraineeEntity trainee = securityService.getCurrentUser().getTrainee();
                    GroupEntity group = trainee.getGroup();
                    TrainerEntity traineesTrainer = group.getTrainer();
                    StreamEntity stream = group.getStream();
                    Set<FeedbackEntity> traineesFeedbackSheets = trainee.getFeedbacks();
                    int duration = stream.getDuration();
                    LocalDate startDate = group.getStartDate().toLocalDate();
                    LocalDate currentDate = LocalDate.now();
                    long currentWeek = ChronoUnit.WEEKS.between(startDate, currentDate) + 1;

                    if (currentWeek > duration) {
                        currentWeek = duration;
                    }

                    Map<Long, FeedbackEntity> feedbackMappedToWeek = new HashMap<>();
                    for (FeedbackEntity feedback:traineesFeedbackSheets) {
                        long feedbackWeek = ChronoUnit.WEEKS.between(startDate, feedback.getDeadline().toLocalDate()) + 1;
                        if (feedbackWeek <= currentWeek) {
                            feedbackMappedToWeek.put(feedbackWeek, feedback);
                        }
                    }

                    model.addAttribute("trainee", trainee);
                    model.addAttribute("trainer", traineesTrainer);
                    model.addAttribute("group", group);
                    model.addAttribute("stream", stream);
                    model.addAttribute("currentWeek", currentWeek);
                    model.addAttribute("feedbackWeeks", feedbackMappedToWeek);
                    break;
            }
            return "index";
        }
        return "login";
    }
}

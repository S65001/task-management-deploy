package com.foresight.taskmanagmentservicebackend.taskmanagmentservicebackend.service;

import com.foresight.taskmanagmentservicebackend.taskmanagmentservicebackend.collection.TaskCollection;
import com.foresight.taskmanagmentservicebackend.taskmanagmentservicebackend.dto.CreateTaskRequest;
import com.foresight.taskmanagmentservicebackend.taskmanagmentservicebackend.dto.TaskSearchCriteria;
import com.foresight.taskmanagmentservicebackend.taskmanagmentservicebackend.dto.TaskSummary;
import com.foresight.taskmanagmentservicebackend.taskmanagmentservicebackend.exception.ErrorCode;
import com.foresight.taskmanagmentservicebackend.taskmanagmentservicebackend.exception.RuntimeErrorCodedException;
import com.foresight.taskmanagmentservicebackend.taskmanagmentservicebackend.model.Notification;
import com.foresight.taskmanagmentservicebackend.taskmanagmentservicebackend.model.NotificationMessages;
import com.foresight.taskmanagmentservicebackend.taskmanagmentservicebackend.model.Task;
import com.foresight.taskmanagmentservicebackend.taskmanagmentservicebackend.mapper.TaskMapper;
import com.foresight.taskmanagmentservicebackend.taskmanagmentservicebackend.repo.TaskCollectionRepo;
import lombok.AllArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import org.springframework.data.domain.Pageable;

import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
@AllArgsConstructor
public class TaskService {
    private TaskMapper mapper;
    private TaskCollectionRepo taskCollectionRepo;
    private UserService userService;
    private TeamService teamService;
    private NotificationService notificationService;
    private MongoTemplate mongoTemplate;

    public void createTask(CreateTaskRequest taskCollection, String teamId) {
        TaskCollection task = taskCollectionRepo.save(mapper.createTaskRequestToTaskCollection( taskCollection));
        Task task1 = mapper.taskCollectionToTask(task);
        userService.addTask(task1, taskCollection.getAssignee().getMemberId());
        teamService.addTask(task1, teamId);
        notificationService.pushUserNotification(taskCollection.getAssignee().getMemberId(), Notification.builder()
                .notificationId(UUID.randomUUID().toString())
                .receiver(taskCollection.getAssignee().getMemberId())
                .content(NotificationMessages.TASK_CREATED.getMessage(taskCollection.getCreator().getFirstname()+" "+taskCollection.getCreator().getLastname()))
                .issuedDate(new Date())
                .build());
    }

    public void editTask(TaskCollection taskCollection, String teamId) {
        taskCollectionRepo.findById(taskCollection.getTaskId()).orElseThrow(() -> new RuntimeErrorCodedException(ErrorCode.TASK_NOT_FOUND_EXCEPTION));
        taskCollectionRepo.save(taskCollection);
        Task task = mapper.taskCollectionToTask(taskCollection);
        userService.editTask(task);
        teamService.editTask(task, teamId);
        notificationService.pushUserNotification(taskCollection.getAssignee().getMemberId(), Notification.builder()
                .notificationId(UUID.randomUUID().toString())
                .receiver(taskCollection.getAssignee().getMemberId())
                .content(NotificationMessages.TASK_UPDATED.getMessage(taskCollection.getTitle()))
                .issuedDate(new Date())
                .build());
    }

    public TaskCollection getTask(String id) {
        return taskCollectionRepo.findById(id).orElseThrow(() -> new RuntimeErrorCodedException(ErrorCode.TASK_NOT_FOUND_EXCEPTION));
    }

    public Page<TaskSummary> getSummaries(Pageable pageable) {
        List<TaskSummary> summaries = mongoTemplate.find(new Query().with(pageable), TaskSummary.class, "task");
        long count = mongoTemplate.count(new Query().with(pageable), TaskSummary.class, "task");
        return new PageImpl<>(summaries, pageable, count);
    }

    public Page<TaskSummary> searchSummaries(Pageable pageable, TaskSearchCriteria criteria) {
        Query query = new Query();
        if (criteria.getTitle() != null || !criteria.getTitle().isEmpty()) {
            query.addCriteria(Criteria.where("title").is(criteria.getTitle()));
        }
        if (criteria.getStatus() != null) {
            query.addCriteria(Criteria.where("status").is(criteria.getStatus()));
        }
        if (criteria.getPriority() != null) {
            query.addCriteria(Criteria.where("priority").is(criteria.getPriority()));
        }
        if (criteria.getStartDate() != null) {
            query.addCriteria(Criteria.where("startDate").is(criteria.getStartDate()));
        }
        if (criteria.getEndDate() != null) {
            query.addCriteria(Criteria.where("endDate").is(criteria.getEndDate()));
        }
        if( (criteria.getCreatorFirstname()!=null && !criteria.getCreatorFirstname().isEmpty()))
        {
            query.addCriteria(Criteria.where("creator.firstname").is(criteria.getCreatorFirstname()));
        }
        if( (criteria.getCreatorLastname()!=null && !criteria.getCreatorLastname().isEmpty()))
        {
            query.addCriteria(Criteria.where("creator.lastname").is(criteria.getCreatorLastname()));
        }

        if( (criteria.getAssigneeFirstname()!=null && !criteria.getAssigneeFirstname().isEmpty()))
        {
            query.addCriteria(Criteria.where("assignee.firstname").is(criteria.getCreatorFirstname()));
        }

        if( (criteria.getAssigneeLastname()!=null && !criteria.getAssigneeLastname().isEmpty()))
        {
            query.addCriteria(Criteria.where("assignee.lastname").is(criteria.getCreatorLastname()));
        }
        List<TaskSummary> summaries = mongoTemplate.find(query.with(pageable), TaskSummary.class, "task");
        long count = mongoTemplate.count(new Query().with(pageable), TaskSummary.class, "task");
        return new PageImpl<>(summaries, pageable, count);

    }

    public void deleteTask(String teamId, String taskId) {
        TaskCollection taskCollection= taskCollectionRepo.findById(taskId).orElseThrow(()->new RuntimeException("task is not found"));
        userService.deleteTask(taskCollection.getAssignee().getMemberId(),taskId);
        teamService.deleteTeamTask(teamId,taskId);
        taskCollectionRepo.deleteById(taskId);
        notificationService.pushUserNotification(taskCollection.getAssignee().getMemberId(), Notification.builder()
                .notificationId(UUID.randomUUID().toString())
                .receiver(taskCollection.getAssignee().getMemberId())
                .content(NotificationMessages.TASK_DELETED.getMessage(taskCollection.getTitle()))
                .issuedDate(new Date())
                .build());
    }
}

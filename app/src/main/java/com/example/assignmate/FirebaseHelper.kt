package com.example.assignmate

import android.content.Context
import android.net.Uri
import android.util.Log
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.example.assignmate.model.Comment
import com.example.assignmate.model.Group
import com.example.assignmate.model.Label
import com.example.assignmate.model.Member
import com.example.assignmate.model.Notification
import com.example.assignmate.model.Subtask
import com.example.assignmate.model.Task
import com.example.assignmate.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import java.text.SimpleDateFormat
import java.util.Locale

class FirebaseHelper {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val groupsCollection = db.collection("groups")
    private val usersCollection = db.collection("users")
    private val tasksCollection = db.collection("tasks")
    private val commentsCollection = db.collection("comments")
    // notificationsCollection is removed as we now use users/{userId}/notifications
    private val labelsCollection = db.collection("labels")


    fun loginUser(email: String, password: String, onSuccess: (String) -> Unit, onFailure: (Exception) -> Unit) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                val userId = auth.currentUser?.uid ?: ""
                onSuccess(userId)
            }
            .addOnFailureListener { e ->
                onFailure(e)
            }
    }

    fun registerUser(
        username: String,
        email: String,
        password: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->
                val userId = authResult.user?.uid ?: ""
                val user = User(id = userId, username = username, email = email)
                usersCollection.document(userId).set(user)
                    .addOnSuccessListener { onSuccess() }
                    .addOnFailureListener { e -> onFailure(e) }
            }
            .addOnFailureListener { e ->
                onFailure(e)
            }
    }

    fun sendPasswordResetEmail(email: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        auth.sendPasswordResetEmail(email)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onFailure(e) }
    }

    fun updateUsername(userId: String, username: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        usersCollection.document(userId).update("username", username)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onFailure(e) }
    }

    fun updateUserProfile(userId: String, username: String, profileImage: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val updates = mapOf(
            "username" to username,
            "profileImage" to profileImage
        )
        usersCollection.document(userId).update(updates)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onFailure(e) }
    }

    fun createGroup(
        groupName: String,
        groupDescription: String,
        leaderId: String,
        groupCode: String,
        profileImage: String = "",
        onSuccess: (String) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val members = mapOf(leaderId to "leader")
        val group = Group(
            name = groupName,
            description = groupDescription,
            leaderId = leaderId,
            code = groupCode,
            members = members,
            lastUpdated = System.currentTimeMillis(),
            profileImage = profileImage
        )

        groupsCollection.add(group)
            .addOnSuccessListener { documentReference ->
                onSuccess(documentReference.id)
            }
            .addOnFailureListener { e ->
                onFailure(e)
            }
    }

    fun getGroupsForUser(userId: String, onSuccess: (List<Group>) -> Unit, onFailure: (Exception) -> Unit) {
        groupsCollection.whereGreaterThanOrEqualTo("members.$userId", "")
            .get()
            .addOnSuccessListener { querySnapshot ->
                val groups = querySnapshot.toObjects(Group::class.java)
                onSuccess(groups)
            }
            .addOnFailureListener { e ->
                onFailure(e)
            }
    }

    fun joinGroup(
        groupCode: String,
        userId: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        groupsCollection.whereEqualTo("code", groupCode)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (querySnapshot.isEmpty) {
                    onFailure(Exception("Invalid group code"))
                    return@addOnSuccessListener
                }
                val groupDoc = querySnapshot.documents.first()
                val group = groupDoc.toObject(Group::class.java)
                val newMember = mapOf("members.$userId" to "member")
                groupsCollection.document(groupDoc.id).update(newMember)
                    .addOnSuccessListener {
                        // Notify group leader about the new member
                        getUserDetails(userId, { user ->
                            if (group != null) {
                                notifyLeaders(
                                    groupId = group.id,
                                    title = "Member Joined",
                                    content = "${user?.username} joined ${group.name}",
                                    excludeUserId = userId
                                )
                            }
                        }, {})
                        onSuccess()
                    }
                    .addOnFailureListener { e ->
                        onFailure(e)
                    }
            }
            .addOnFailureListener { e ->
                onFailure(e)
            }
    }

    fun updateGroup(
        groupId: String,
        groupName: String,
        groupDescription: String,
        profileImage: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val updates = mapOf(
            "name" to groupName,
            "description" to groupDescription,
            "lastUpdated" to System.currentTimeMillis(),
            "profileImage" to profileImage
        )
        groupsCollection.document(groupId).update(updates)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { e ->
                onFailure(e)
            }
    }

    fun deleteGroup(groupId: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        groupsCollection.document(groupId).delete()
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { e ->
                onFailure(e)
            }
    }

    fun setFavourite(groupId: String, userId: String, isFavourite: Boolean, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val update = if (isFavourite) {
            FieldValue.arrayUnion(userId)
        } else {
            FieldValue.arrayRemove(userId)
        }
        groupsCollection.document(groupId).update("favouriteBy", update)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onFailure(e) }
    }

    fun getUserDetails(userId: String, onSuccess: (User?) -> Unit, onFailure: (Exception) -> Unit) {
        usersCollection.document(userId).get()
            .addOnSuccessListener { documentSnapshot ->
                val user = documentSnapshot.toObject(User::class.java)
                onSuccess(user)
            }
            .addOnFailureListener { e ->
                onFailure(e)
            }
    }

    fun getUsers(userIds: List<String>, onSuccess: (List<User>) -> Unit, onFailure: (Exception) -> Unit) {
        if (userIds.isEmpty()) {
            onSuccess(emptyList())
            return
        }
        usersCollection.whereIn(FieldPath.documentId(), userIds).get()
            .addOnSuccessListener { querySnapshot ->
                val users = querySnapshot.toObjects(User::class.java)
                onSuccess(users)
            }
            .addOnFailureListener { e ->
                onFailure(e)
            }
    }

    fun getTotalTasksForUser(userId: String, onSuccess: (Int) -> Unit, onFailure: (Exception) -> Unit) {
        tasksCollection.whereArrayContains("assignedTo", userId)
            .get()
            .addOnSuccessListener { querySnapshot ->
                onSuccess(querySnapshot.size())
            }
            .addOnFailureListener { e ->
                onFailure(e)
            }
    }

    fun getPendingTasksForUser(userId: String, onSuccess: (Int) -> Unit, onFailure: (Exception) -> Unit) {
        tasksCollection.whereArrayContains("assignedTo", userId)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val pendingTasks = querySnapshot.toObjects(Task::class.java).count { it.status != "Complete" }
                onSuccess(pendingTasks)
            }
            .addOnFailureListener { e ->
                onFailure(e)
            }
    }

    fun getDueTasksForUser(userId: String, onSuccess: (Int) -> Unit, onFailure: (Exception) -> Unit) {
        tasksCollection.whereArrayContains("assignedTo", userId)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val calendar = java.util.Calendar.getInstance()
                calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
                calendar.set(java.util.Calendar.MINUTE, 0)
                calendar.set(java.util.Calendar.SECOND, 0)
                calendar.set(java.util.Calendar.MILLISECOND, 0)
                val todayStart = calendar.timeInMillis

                val dueTasks = querySnapshot.toObjects(Task::class.java).count {
                    it.dueDate != 0L && it.dueDate < todayStart && !it.status.equals("Complete", ignoreCase = true)
                }
                onSuccess(dueTasks)
            }
            .addOnFailureListener { e ->
                onFailure(e)
            }
    }

    fun getUpcomingTasksForUser(
        userId: String,
        includeLeaderTasks: Boolean,
        onSuccess: (Map<Group, List<Task>>, List<Task>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val groups = suspendCoroutine<List<Group>> { continuation ->
                    getGroupsForUser(userId, { continuation.resume(it) }, { continuation.resumeWithException(it) })
                }

                val leaderGroups = groups.filter {
                    val role = it.members[userId]
                    role == "leader" || role == "co-leader"
                }

                val leaderTasksDeferred = leaderGroups.map { group ->
                    async(Dispatchers.IO) {
                        val tasks = suspendCoroutine<List<Task>> { continuation ->
                            getTasksForGroup(group.id, { continuation.resume(it) }, { continuation.resumeWithException(it) })
                        }
                        val upcoming = tasks.filter { task ->
                            val currentTime = System.currentTimeMillis()
                            val sevenDaysInMillis = 7L * 24 * 60 * 60 * 1000
                            val sevenDaysFromNow = currentTime + sevenDaysInMillis
                            task.dueDate > currentTime && task.dueDate <= sevenDaysFromNow && !task.status.equals("Complete", ignoreCase = true)
                        }
                        group to upcoming
                    }
                }

                val userTasksDeferred = async(Dispatchers.IO) {
                    suspendCoroutine<List<Task>> { continuation ->
                        getUpcomingTasksForUser(userId, { continuation.resume(it) }, { continuation.resumeWithException(it) })
                    }
                }

                val leaderTasksResult = leaderTasksDeferred.map { it.await() }.toMap()
                val userTasksResult = userTasksDeferred.await()

                val filteredUserTasks = userTasksResult.filter { task ->
                    !leaderTasksResult.any { (_, tasks) -> tasks.any { it.uid == task.uid } }
                }

                onSuccess(leaderTasksResult, filteredUserTasks)

            } catch (e: Exception) {
                onFailure(e)
            }
        }
    }

    fun getUpcomingTasksForUser(userId: String, onSuccess: (List<Task>) -> Unit, onFailure: (Exception) -> Unit) {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val todayMillis = calendar.timeInMillis

        val endCalendar = calendar.clone() as Calendar
        endCalendar.add(Calendar.DATE, 7)
        endCalendar.set(Calendar.HOUR_OF_DAY, 23)
        endCalendar.set(Calendar.MINUTE, 59)
        endCalendar.set(Calendar.SECOND, 59)
        endCalendar.set(Calendar.MILLISECOND, 999)
        val endMillis = endCalendar.timeInMillis

        tasksCollection.whereArrayContains("assignedTo", userId)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val upcomingTasks = querySnapshot.toObjects(Task::class.java).filter {
                    val isUpcoming = it.dueDate >= todayMillis && it.dueDate <= endMillis
                    val isNotCompleted = !it.status.equals("Complete", ignoreCase = true) && !it.status.equals("Completed", ignoreCase = true)
                    isUpcoming && isNotCompleted
                }.sortedBy { it.dueDate }
                onSuccess(upcomingTasks)
            }
            .addOnFailureListener { e ->
                onFailure(e)
            }
    }

    fun getTasksForGroup(groupId: String, onSuccess: (List<Task>) -> Unit, onFailure: (Exception) -> Unit) {
        tasksCollection.whereEqualTo("groupId", groupId)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val tasks = querySnapshot.toObjects(Task::class.java)
                onSuccess(tasks)
            }
            .addOnFailureListener { e ->
                onFailure(e)
            }
    }

    fun getAllTasksForUser(userId: String, onSuccess: (List<Task>) -> Unit, onFailure: (Exception) -> Unit) {
        tasksCollection.whereArrayContains("assignedTo", userId)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val tasks = querySnapshot.toObjects(Task::class.java)
                onSuccess(tasks)
            }
            .addOnFailureListener { e ->
                onFailure(e)
            }
    }

    fun deleteTask(taskId: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        tasksCollection.document(taskId).delete()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onFailure(e) }
    }

    fun getGroup(groupId: String, onSuccess: (Group?) -> Unit, onFailure: (Exception) -> Unit) {
        groupsCollection.document(groupId).get()
            .addOnSuccessListener { documentSnapshot ->
                val group = documentSnapshot.toObject(Group::class.java)
                onSuccess(group)
            }
            .addOnFailureListener { e ->
                onFailure(e)
            }
    }

    fun getGroupMembers(groupId: String, onSuccess: (List<Member>) -> Unit, onFailure: (Exception) -> Unit) {
        groupsCollection.document(groupId).get()
            .addOnSuccessListener { documentSnapshot ->
                val group = documentSnapshot.toObject(Group::class.java)
                if (group != null) {
                    val memberIds = group.members.keys.toList()
                    if (memberIds.isEmpty()) {
                        onSuccess(emptyList())
                        return@addOnSuccessListener
                    }
                    usersCollection.whereIn(FieldPath.documentId(), memberIds).get()
                        .addOnSuccessListener { usersSnapshot ->
                            val users = usersSnapshot.toObjects(User::class.java)
                            val members = users.map { user ->
                                Member(id = user.id, name = user.username, role = group.members[user.id] ?: "")
                            }
                            onSuccess(members)
                        }
                        .addOnFailureListener { e -> onFailure(e) }
                } else {
                    onFailure(Exception("Group not found"))
                }
            }
            .addOnFailureListener { e ->
                onFailure(e)
            }
    }

    fun removeMemberFromGroup(groupId: String, userId: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        // Notification Logic before removal
        val currentActorId = auth.currentUser?.uid ?: ""
        getUserDetails(userId, { removedUser ->
            getGroup(groupId, { group ->
                getUserDetails(currentActorId, { actor ->
                    if (group != null && removedUser != null) {
                        val title = if (userId == currentActorId) "Member Left" else "Member Removed"
                        val action = if (userId == currentActorId) "left" else "was removed from"
                        val byText = if (userId != currentActorId) " by ${actor?.username}" else ""
                        
                        notifyLeaders(
                            groupId = groupId,
                            title = title,
                            content = "${removedUser.username} $action ${group.name}$byText",
                            excludeUserId = currentActorId
                        )
                    }
                }, {})
            }, {})
        }, {})

        val updates = mapOf("members.$userId" to FieldValue.delete())
        groupsCollection.document(groupId).update(updates)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onFailure(e) }
    }

    fun updateMemberRole(groupId: String, userId: String, role: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val updates = mapOf("members.$userId" to role)
        groupsCollection.document(groupId).update(updates)
            .addOnSuccessListener {
                if (role == "co-leader") {
                    getGroup(groupId, { group ->
                        group?.let {
                             val notification = Notification(
                                userId = userId,
                                message = "Role Update: You have been promoted to co-leader in ${it.name}",
                                timestamp = System.currentTimeMillis()
                            )
                            addNotification(notification, {}, {})
                        }
                    }, {})
                }
                onSuccess()
            }
            .addOnFailureListener { e -> onFailure(e) }
    }

    fun getTask(taskId: String, onSuccess: (Task?) -> Unit, onFailure: (Exception) -> Unit) {
        tasksCollection.document(taskId).get()
            .addOnSuccessListener { documentSnapshot ->
                val task = documentSnapshot.toObject(Task::class.java)
                onSuccess(task)
            }
            .addOnFailureListener { e ->
                onFailure(e)
            }
    }

    fun updateTask(
        taskId: String,
        title: String,
        description: String?,
        dueDate: Long?,
        status: String,
        assignedTo: List<String>?,
        subtasks: List<Subtask>,
        labels: List<String>,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val currentUserId = auth.currentUser?.uid ?: ""

        getTask(taskId, { oldTask ->
            if (oldTask == null) {
                onFailure(Exception("Task not found for update"))
                return@getTask
            }

            val updates = mutableMapOf<String, Any?>(
                "name" to title,
                "description" to description,
                "dueDate" to dueDate,
                "status" to status,
                "assignedTo" to assignedTo,
                "subtasks" to subtasks,
                "labels" to labels
            )
            
            tasksCollection.document(taskId).update(updates)
                .addOnSuccessListener {
                    getUserDetails(currentUserId, { currentUser ->
                        val actorName = currentUser?.username ?: "Someone"
                        
                        // 1. Status Changes
                        if (oldTask.status != status) {
                            if (status.equals("Complete", ignoreCase = true)) {
                                // Task Completed
                                notifyLeaders(oldTask.groupId, "Task Completed", "$actorName completed task '$title' in ${oldTask.groupName}", excludeUserId = currentUserId, taskId = taskId)
                                assignedTo?.let {
                                     notifyUsers(it, "Task Completed", "Task '$title' in ${oldTask.groupName} was completed by $actorName", excludeUserId = currentUserId, taskId = taskId)
                                }
                            } else if (oldTask.status.equals("Complete", ignoreCase = true)) {
                                // Task Reopened
                                assignedTo?.let {
                                    notifyUsers(it, "Task Reopened", "Task '$title' in ${oldTask.groupName} was reopened by $actorName", excludeUserId = currentUserId, taskId = taskId)
                                }
                            } else {
                                // Status Update (e.g., In Progress)
                                assignedTo?.let {
                                     notifyUsers(it, "Task Updated", "Status of task '$title' in ${oldTask.groupName} changed to $status by $actorName", excludeUserId = currentUserId, taskId = taskId)
                                }
                            }
                        }
                        
                        // 2. Deadline Changes
                        if (oldTask.dueDate != dueDate && dueDate != null) {
                            val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                            val dateString = formatter.format(java.util.Date(dueDate))
                            assignedTo?.let {
                                notifyUsers(it, "Deadline Changed", "Deadline for task '$title' in ${oldTask.groupName} changed to $dateString by $actorName", excludeUserId = currentUserId, taskId = taskId)
                            }
                        }

                        // 3. Assignment Changes
                        assignedTo?.forEach { userId ->
                            if (!oldTask.assignedTo.contains(userId)) {
                                // New Assignment
                                notifyUsers(listOf(userId), "New Task Assigned", "You were assigned the task '$title' in ${oldTask.groupName} by $actorName", excludeUserId = currentUserId, taskId = taskId)
                            }
                        }
                        
                        // 4. General Edit (if not completed and not just created)
                        // Trigger 'Task Edited' for leaders if it's a significant edit and not just a status change we already notified about
                        if (status == oldTask.status && (title != oldTask.name || description != oldTask.description)) {
                             notifyLeaders(oldTask.groupId, "Task Edited", "$actorName edited task '$title' in ${oldTask.groupName}", excludeUserId = currentUserId, taskId = taskId)
                             assignedTo?.let {
                                 notifyUsers(it, "Task Updated", "Details for task '$title' in ${oldTask.groupName} were updated by $actorName", excludeUserId = currentUserId, taskId = taskId)
                             }
                        }

                    }, {})
                    onSuccess()
                }
                .addOnFailureListener { e -> onFailure(e) }
        }, { e -> onFailure(e) })
    }

    fun getCommentsForTask(taskId: String, onSuccess: (List<Comment>) -> Unit, onFailure: (Exception) -> Unit) {
        commentsCollection.whereEqualTo("taskId", taskId).get()
            .addOnSuccessListener { querySnapshot ->
                val comments = querySnapshot.toObjects(Comment::class.java)
                onSuccess(comments)
            }
            .addOnFailureListener { e -> onFailure(e) }
    }

    fun addComment(comment: Comment, onSuccess: (String) -> Unit, onFailure: (Exception) -> Unit) {
        val currentUserId = auth.currentUser?.uid ?: ""
        
        getUserDetails(comment.userId,
            onSuccess = { user ->
                val commentWithUsername = comment.copy(username = user?.username ?: "Unknown User")
                commentsCollection.add(commentWithUsername)
                    .addOnSuccessListener { documentReference ->
                        getTask(comment.taskId, { task ->
                            if (task != null) {
                                val actorName = user?.username ?: "Someone"
                                val taskName = task.name
                                val groupName = task.groupName // Assumption: task has groupName. If not, fetch group. 
                                // Task model has groupName.
                                
                                // Notify Assignees
                                notifyUsers(
                                    task.assignedTo, 
                                    "New Comment", 
                                    "$actorName commented on '$taskName'", 
                                    excludeUserId = currentUserId, 
                                    taskId = comment.taskId
                                )
                                
                                // Notify Leaders
                                notifyLeaders(
                                    task.groupId,
                                    "New Comment",
                                    "$actorName commented on '$taskName'",
                                    excludeUserId = currentUserId,
                                    taskId = comment.taskId
                                )
                            }
                        }, {})
                        
                        onSuccess(documentReference.id)
                    }
                    .addOnFailureListener { e -> onFailure(e) }
            },
            onFailure = {
                 commentsCollection.add(comment)
                    .addOnSuccessListener { documentReference ->
                        onSuccess(documentReference.id)
                    }
                    .addOnFailureListener { e -> onFailure(e) }
            }
        )
    }

    fun deleteComments(commentIds: List<String>, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val batch = db.batch()
        commentIds.forEach { id ->
            val docRef = commentsCollection.document(id)
            batch.delete(docRef)
        }
        batch.commit()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onFailure(e) }
    }

    fun addNotification(notification: Notification, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        // Send to users/{userId}/notifications instead of global notifications
        usersCollection.document(notification.userId).collection("notifications").add(notification)
            .addOnSuccessListener { 
                Log.d("FirebaseHelper", "Notification added successfully")
                onSuccess()
             }
            .addOnFailureListener { e -> 
                Log.e("FirebaseHelper", "Failed to add notification", e)
                onFailure(e) 
            }
    }

    fun getNotificationsForUser(userId: String, onSuccess: (List<Notification>) -> Unit, onFailure: (Exception) -> Unit) {
        usersCollection.document(userId).collection("notifications").get()
            .addOnSuccessListener { querySnapshot ->
                val notifications = querySnapshot.toObjects(Notification::class.java).sortedByDescending { it.timestamp }
                onSuccess(notifications)
            }
            .addOnFailureListener { e -> onFailure(e) }
    }
    
    // NEW: Real-time listener for notifications
    fun listenToNotifications(userId: String, onSuccess: (List<Notification>) -> Unit, onFailure: (Exception) -> Unit): ListenerRegistration {
        return usersCollection.document(userId).collection("notifications")
            .addSnapshotListener { querySnapshot, e ->
                if (e != null) {
                    onFailure(e)
                    return@addSnapshotListener
                }
                if (querySnapshot != null) {
                     val notifications = querySnapshot.toObjects(Notification::class.java).sortedByDescending { it.timestamp }
                     onSuccess(notifications)
                }
            }
    }

    fun markNotificationAsRead(notificationId: String, userId: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        usersCollection.document(userId).collection("notifications").document(notificationId).update("isRead", true)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onFailure(e) }
    }

    fun markAllNotificationsAsRead(userId: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        usersCollection.document(userId).collection("notifications").get()
            .addOnSuccessListener { querySnapshot ->
                val batch = db.batch()
                for (document in querySnapshot.documents) {
                    batch.update(document.reference, "isRead", true)
                }
                batch.commit()
                    .addOnSuccessListener { onSuccess() }
                    .addOnFailureListener { e -> onFailure(e) }
            }
            .addOnFailureListener { e -> onFailure(e) }
    }

    fun deleteNotification(notificationId: String, userId: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        usersCollection.document(userId).collection("notifications").document(notificationId).delete()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onFailure(e) }
    }

    fun deleteAllNotifications(userId: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        usersCollection.document(userId).collection("notifications").get()
            .addOnSuccessListener { querySnapshot ->
                val batch = db.batch()
                for (document in querySnapshot.documents) {
                    batch.delete(document.reference)
                }
                batch.commit()
                    .addOnSuccessListener { onSuccess() }
                    .addOnFailureListener { e -> onFailure(e) }
            }
            .addOnFailureListener { e -> onFailure(e) }
    }

    fun getUnreadNotificationCount(userId: String, onSuccess: (Int) -> Unit, onFailure: (Exception) -> Unit) {
        usersCollection.document(userId).collection("notifications").whereEqualTo("isRead", false).get()
            .addOnSuccessListener { querySnapshot ->
                onSuccess(querySnapshot.size())
            }
            .addOnFailureListener { e -> onFailure(e) }
    }

    fun createTask(task: Task, onSuccess: (String) -> Unit, onFailure: (Exception) -> Unit) {
        tasksCollection.add(task)
            .addOnSuccessListener { documentReference ->
                val currentUserId = auth.currentUser?.uid ?: ""
                getUserDetails(currentUserId, { user ->
                    val actorName = user?.username ?: "Someone"
                    
                    // Notify Assigned Users
                    notifyUsers(
                        task.assignedTo,
                        "New Task Assigned",
                        "You were assigned the task '${task.name}' in group '${task.groupName}' by $actorName",
                        excludeUserId = currentUserId,
                        taskId = documentReference.id
                    )
                    
                    // Notify Leaders
                    notifyLeaders(
                        task.groupId,
                        "New Task Created",
                        "$actorName created task '${task.name}' in ${task.groupName}",
                        excludeUserId = currentUserId,
                        taskId = documentReference.id
                    )
                }, {})

                onSuccess(documentReference.id)
            }
            .addOnFailureListener { e -> onFailure(e) }
    }

    fun getUserByEmail(email: String, onSuccess: (User?) -> Unit, onFailure: (Exception) -> Unit) {
        usersCollection.whereEqualTo("email", email).get()
            .addOnSuccessListener { querySnapshot ->
                if (querySnapshot.isEmpty) {
                    onSuccess(null)
                } else {
                    val user = querySnapshot.documents.first().toObject(User::class.java)
                    onSuccess(user)
                }
            }
            .addOnFailureListener { e ->
                onFailure(e)
            }
    }

    fun getLabelsForGroup(groupId: String, onSuccess: (List<Label>) -> Unit, onFailure: (Exception) -> Unit) {
        labelsCollection.whereEqualTo("groupId", groupId).get()
            .addOnSuccessListener { querySnapshot ->
                val labels = querySnapshot.toObjects(Label::class.java)
                onSuccess(labels)
            }
            .addOnFailureListener { e -> onFailure(e) }
    }

    fun addLabel(label: Label, onSuccess: (String) -> Unit, onFailure: (Exception) -> Unit) {
        labelsCollection.add(label)
            .addOnSuccessListener { documentReference ->
                onSuccess(documentReference.id)
            }
            .addOnFailureListener { e -> onFailure(e) }
    }

    fun updateLabel(labelId: String, name: String, color: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val updates = mapOf(
            "name" to name,
            "color" to color
        )
        labelsCollection.document(labelId).update(updates)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onFailure(e) }
    }

    fun deleteLabel(labelId: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        labelsCollection.document(labelId).delete()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onFailure(e) }
    }

    fun uploadFile(context: Context, uri: Uri, folderName: String, onSuccess: (String) -> Unit, onFailure: (Exception) -> Unit) {
        val contentResolver = context.contentResolver
        val mimeType = contentResolver.getType(uri)
        
        val isImage = mimeType?.startsWith("image/") == true
        val isVideo = mimeType?.startsWith("video/") == true
        
        val resourceType = if (isImage) "image" else if (isVideo) "video" else "raw"
        
        MediaManager.get().upload(uri)
            .unsigned("assignmate_preset")
            .option("folder", folderName)
            .option("resource_type", resourceType)
            .callback(object : UploadCallback {
                override fun onStart(requestId: String) {}

                override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {}

                override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                    val url = (resultData["secure_url"] ?: resultData["url"]) as String
                    onSuccess(url)
                }

                override fun onError(requestId: String, errorInfo: ErrorInfo) {
                    onFailure(Exception(errorInfo.description))
                }

                override fun onReschedule(requestId: String, errorInfo: ErrorInfo) {}
            })
            .dispatch()
    }
    
    // Notification Helpers
    private fun notifyLeaders(groupId: String, title: String, content: String, excludeUserId: String? = null, taskId: String = "") {
         getGroup(groupId, { group ->
             group?.members?.forEach { (memberId, role) ->
                 if ((role == "leader" || role == "co-leader") && memberId != excludeUserId) {
                      val notification = Notification(
                         userId = memberId,
                         message = "$title: $content",
                         taskId = taskId,
                         timestamp = System.currentTimeMillis()
                     )
                     addNotification(notification, {}, {})
                 }
             }
         }, {})
    }

    private fun notifyUsers(userIds: List<String>, title: String, content: String, excludeUserId: String? = null, taskId: String = "") {
        userIds.forEach { userId ->
            if (userId != excludeUserId) {
                val notification = Notification(
                     userId = userId,
                     message = "$title: $content",
                     taskId = taskId,
                     timestamp = System.currentTimeMillis()
                 )
                 addNotification(notification, {}, {})
            }
        }
    }
}

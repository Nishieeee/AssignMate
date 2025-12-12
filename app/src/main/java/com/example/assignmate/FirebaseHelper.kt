package com.example.assignmate

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

class FirebaseHelper {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val groupsCollection = db.collection("groups")
    private val usersCollection = db.collection("users")
    private val tasksCollection = db.collection("tasks")
    private val commentsCollection = db.collection("comments")
    private val notificationsCollection = db.collection("notifications")
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

    fun createGroup(
        groupName: String,
        groupDescription: String,
        leaderId: String,
        groupCode: String,
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
            lastUpdated = System.currentTimeMillis()
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
                val newMember = mapOf("members.$userId" to "member")
                groupsCollection.document(groupDoc.id).update(newMember)
                    .addOnSuccessListener {
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
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val updates = mapOf(
            "name" to groupName,
            "description" to groupDescription,
            "lastUpdated" to System.currentTimeMillis()
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
                val currentTime = System.currentTimeMillis()
                val threeDaysInMillis = 3 * 24 * 60 * 60 * 1000
                val threeDaysFromNow = currentTime + threeDaysInMillis
                val dueTasks = querySnapshot.toObjects(Task::class.java).count {
                    it.dueDate > currentTime && it.dueDate <= threeDaysFromNow && !it.status.equals("Complete", ignoreCase = true)
                }
                onSuccess(dueTasks)
            }
            .addOnFailureListener { e ->
                onFailure(e)
            }
    }

    fun getUpcomingTasksForUser(userId: String, onSuccess: (List<Task>) -> Unit, onFailure: (Exception) -> Unit) {
        tasksCollection.whereArrayContains("assignedTo", userId)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val currentTime = System.currentTimeMillis()
                val threeDaysInMillis = 3 * 24 * 60 * 60 * 1000
                val threeDaysFromNow = currentTime + threeDaysInMillis
                val upcomingTasks = querySnapshot.toObjects(Task::class.java).filter {
                    it.dueDate > currentTime && it.dueDate <= threeDaysFromNow && !it.status.equals("Complete", ignoreCase = true)
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
        val updates = mapOf("members.$userId" to FieldValue.delete())
        groupsCollection.document(groupId).update(updates)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onFailure(e) }
    }

    fun updateMemberRole(groupId: String, userId: String, role: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val updates = mapOf("members.$userId" to role)
        groupsCollection.document(groupId).update(updates)
            .addOnSuccessListener { onSuccess() }
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
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onFailure(e) }
    }

    fun getCommentsForTask(taskId: String, onSuccess: (List<Comment>) -> Unit, onFailure: (Exception) -> Unit) {
        commentsCollection.whereEqualTo("taskId", taskId).get()
            .addOnSuccessListener { querySnapshot ->
                val comments = querySnapshot.toObjects(Comment::class.java)
                onSuccess(comments)
            }
            .addOnFailureListener { e -> onFailure(e) }
    }

    fun addComment(comment: Comment, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        commentsCollection.add(comment)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onFailure(e) }
    }

    fun addNotification(notification: Notification, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        notificationsCollection.add(notification)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onFailure(e) }
    }

    fun getNotificationsForUser(userId: String, onSuccess: (List<Notification>) -> Unit, onFailure: (Exception) -> Unit) {
        notificationsCollection.whereEqualTo("userId", userId).get()
            .addOnSuccessListener { querySnapshot ->
                val notifications = querySnapshot.toObjects(Notification::class.java)
                onSuccess(notifications)
            }
            .addOnFailureListener { e -> onFailure(e) }
    }

    fun markNotificationAsRead(notificationId: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        notificationsCollection.document(notificationId).update("read", true)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onFailure(e) }
    }

    fun markAllNotificationsAsRead(userId: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        notificationsCollection.whereEqualTo("userId", userId).get()
            .addOnSuccessListener { querySnapshot ->
                val batch = db.batch()
                for (document in querySnapshot.documents) {
                    batch.update(document.reference, "read", true)
                }
                batch.commit()
                    .addOnSuccessListener { onSuccess() }
                    .addOnFailureListener { e -> onFailure(e) }
            }
            .addOnFailureListener { e -> onFailure(e) }
    }

    fun deleteNotification(notificationId: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        notificationsCollection.document(notificationId).delete()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onFailure(e) }
    }

    fun deleteAllNotifications(userId: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        notificationsCollection.whereEqualTo("userId", userId).get()
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
        notificationsCollection.whereEqualTo("userId", userId).whereEqualTo("read", false).get()
            .addOnSuccessListener { querySnapshot ->
                onSuccess(querySnapshot.size())
            }
            .addOnFailureListener { e -> onFailure(e) }
    }

    fun createTask(task: Task, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        tasksCollection.add(task)
            .addOnSuccessListener { onSuccess() }
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

    fun addLabel(label: Label, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        labelsCollection.add(label)
            .addOnSuccessListener { onSuccess() }
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
}

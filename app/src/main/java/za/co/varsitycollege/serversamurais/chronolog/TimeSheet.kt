package za.co.varsitycollege.serversamurais.chronolog

import SharedViewModel
import DateRangePickerFragment
import android.app.Activity
import android.app.DatePickerDialog
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.os.CountDownTimer
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.CheckBox
import android.widget.DatePicker
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import za.co.varsitycollege.serversamurais.chronolog.Helpers.FirebaseHelper
import za.co.varsitycollege.serversamurais.chronolog.adapters.TaskAdapter
import za.co.varsitycollege.serversamurais.chronolog.model.Category
import za.co.varsitycollege.serversamurais.chronolog.model.Task
import za.co.varsitycollege.serversamurais.chronolog.views.DurationPickerDialogFragment
import java.util.Calendar
import java.util.Date
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointForward
import kotlinx.coroutines.selects.select
import java.text.SimpleDateFormat
import java.util.Locale
import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.provider.Settings
import android.widget.ProgressBar
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import com.google.firebase.storage.FirebaseStorage
import za.co.varsitycollege.serversamurais.chronolog.Helpers.RecyclerAdapter
import za.co.varsitycollege.serversamurais.chronolog.model.Notification
import za.co.varsitycollege.serversamurais.chronolog.model.Team
import za.co.varsitycollege.serversamurais.chronolog.views.recentActivityView
import java.io.File
import java.util.UUID

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [TimeSheet.newInstance] factory method to
 * create an instance of this fragment.
 */
/**
 * A Fragment subclass that represents a time sheet.
 * This fragment is used to manage tasks, categories, teams, and task photos.
 * It also includes a timer for task duration.
 * Implements FirebaseOperationListener for Firebase operations and DurationPickerListener for picking task duration.
 */
class TimeSheet : Fragment(), FirebaseHelper.FirebaseOperationListener,
    DurationPickerDialogFragment.DurationPickerListener {

    // Parameters for fragment initialization
    private var param1: String? = null
    private var param2: String? = null

    // Firebase helper for Firebase operations
    private lateinit var firebaseHelper: FirebaseHelper

    // UI elements for task details, add new task button, navigation bar, category list, and team choice
    private lateinit var enterTaskDetails: RelativeLayout
    private lateinit var addNewTaskButton: LinearLayout
    private lateinit var navBar: BottomNavigationView
    private lateinit var categoryList: LinearLayout
    private lateinit var chooseTeam: LinearLayout

    // UI elements for task name, description, duration, category, date, and team
    private lateinit var taskNameEditText: EditText
    private lateinit var descriptionEditText: EditText
    private lateinit var durationTextView: TextView
    private var taskCategory: String = "No Category"
    private var taskDate: Date = Date() // Initialize with the current date
    private lateinit var taskDateButton: Button

    // UI elements for category and team auto-complete text views and adapters
    private lateinit var autoCompleteTextView: AutoCompleteTextView
    private lateinit var categoriesAdapter: ArrayAdapter<String>
    private lateinit var teamAutoCompleteTextView: AutoCompleteTextView
    private lateinit var teamsAdapter: ArrayAdapter<String>
    private lateinit var teamList: LinearLayout
    private var taskTeam: String = "No Team"

    // UI elements for creating new category and team views
    private lateinit var createCategoryView: LinearLayout
    private lateinit var createTeamView: LinearLayout

    // Data and adapter for notifications
    private val data = mutableListOf<Notification>()
    private lateinit var adapterNotification: RecyclerAdapter

    // UI elements and variables for timer
    private lateinit var timerTextView: TextView
    private var timerRunning = false
    private var timer: CountDownTimer? = null
    private var timeInMilliseconds = 0L

    // UI elements and variables for task RecyclerView and adapter
    private lateinit var taskRecyclerView: RecyclerView
    private lateinit var taskAdapter: TaskAdapter
    private val tasks = mutableListOf<Task>()

    // UI element for date range text view
    private lateinit var dateRangeTextView: TextView

    // UI elements and variables for camera logic
    private lateinit var addPhotoButton: Button
    private lateinit var takePhotoButton: Button
    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var imagePickerLauncher: ActivityResultLauncher<String>
    private lateinit var cameraLauncher: ActivityResultLauncher<Uri>
    private var taskPhoto: String = ""
    private lateinit var progressBar: ProgressBar

    // URI to hold the current photo
    private var currentPhotoUri: Uri? = null

    /**
     * Called when the fragment is being created.
     * @param savedInstanceState If the fragment is being re-created from a previous saved state, this is the state.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }

        // Register for activity result when requesting permission and picking image
        permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val isCameraPermissionGranted = permissions[Manifest.permission.CAMERA] ?: false
            val isWriteStoragePermissionGranted = permissions[Manifest.permission.WRITE_EXTERNAL_STORAGE] ?: false

            if (isCameraPermissionGranted && isWriteStoragePermissionGranted) {

            } else {
                Toast.makeText(context, "Camera and Storage permissions are required to take a photo.", Toast.LENGTH_LONG).show()
            }
        }

        imagePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                // Handle the picked image URI here, such as uploading to Firebase or displaying it
                try {
                    processPickedImage(uri)
                } catch (e: Exception) {
                    Log.e("ImagePicker", "Error processing picked image", e)
                }
            }
        }

        cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) {
                val fileUri = currentPhotoUri
                fileUri?.let {
                    try {
                        processPickedImage(it)
                    } catch (e: Exception) {
                        Log.e("Camera", "Error processing captured image", e)
                    }
                }
            }
        }
    }

    /**
     * Called to have the fragment instantiate its user interface view.
     * @param inflater The LayoutInflater object that can be used to inflate any views in the fragment.
     * @param container If non-null, this is the parent view that the fragment's UI should be attached to.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state as given here.
     * @return Return the View for the fragment's UI, or null.
     */
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_time_sheet, container, false)
        val activity = activity as Activity

        // Initialize the progress bar
        progressBar = view.findViewById(R.id.progressBar)

        // Initialize the add photo button and set its click listener
        addPhotoButton = view.findViewById(R.id.addPhotoButton)
        addPhotoButton.setOnClickListener {
            pickImageFromGallery()
        }

        // Initialize the take photo button and set its click listener
        takePhotoButton = view.findViewById(R.id.takePhotoButton)
        takePhotoButton.setOnClickListener {
            val permissions = arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)

            if (permissions.all { ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED }) {
                val photoFile = File.createTempFile(UUID.randomUUID().toString(), ".jpg", requireContext().cacheDir)
                currentPhotoUri = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.fileprovider", photoFile)
                cameraLauncher.launch(currentPhotoUri)
            } else {
                permissionLauncher.launch(permissions)
            }
        }

        // Initialize the date range text view and set its click listener
        dateRangeTextView = view.findViewById(R.id.dateRangeTextView)
        dateRangeTextView.setOnClickListener {
            showDateRangePicker()
        }

        // Initialize the task details section
        val closeTaskDetailsBtn: ImageButton = view.findViewById(R.id.closeEnterTaskDetailsBtn)
        addNewTaskButton = view.findViewById(R.id.addNewTaskButton)
        enterTaskDetails = view.findViewById(R.id.enterTaskDetails)
        taskDateButton = view.findViewById(R.id.taskDatePicker)

        // Set the click listener for the task date button
        taskDateButton.setOnClickListener {
            showDatePicker()
        }

        // Initialize the adapter for notifications
        adapterNotification = RecyclerAdapter(requireContext(), data)

        // Initialize the category section
        val categoryButton: Button = view.findViewById(R.id.categoryButton)
        categoryList = view.findViewById(R.id.chooseCategoryList)

        // Set the click listener for the category button
        categoryButton.setOnClickListener {
            toggleCategory()
        }

        // Initialize the duration widget
        val showPickerButton: ImageButton = view.findViewById(R.id.editDurationButton)
        showPickerButton.setOnClickListener {
            val picker = DurationPickerDialogFragment()
            picker.listener = this
            picker.show(parentFragmentManager, "durationPicker")
        }

        // Initialize the navigation bar
        navBar = activity.findViewById(R.id.bottomNavigationView)

        // Initialize the timer text view
        timerTextView = view.findViewById(R.id.timerTextView)

        // Set the click listener for the add new task button
        addNewTaskButton.setOnClickListener {
            taskPhoto = ""
            if (!timerRunning) {
                startTimer()
            } else {
                timeInMilliseconds = 0L  // Reset the timer
                updateTimer()
            }
            toggleVisibility()
        }

        // Set the click listener for the close task details button
        closeTaskDetailsBtn.setOnClickListener {
            toggleVisibility()
        }

        // Initialize the Firebase helper
        firebaseHelper = FirebaseHelper(this)
        val addTaskButton: TextView = view.findViewById(R.id.addTaskButton)

        // Get the user ID
        val userId = firebaseHelper.getUserId()

        // Initialize the task RecyclerView and its adapter
        taskRecyclerView = view.findViewById(R.id.recentTasksRecyclerView)
        taskRecyclerView.layoutManager = LinearLayoutManager(context)
        taskAdapter = TaskAdapter(tasks, firebaseHelper)
        taskRecyclerView.adapter = taskAdapter

        // Fetch tasks for the user
        firebaseHelper.fetchTasks(userId, tasks, taskAdapter)

        // Set the click listener for the add task button
        addTaskButton.setOnClickListener {
            if (timerRunning) {
                stopTimer()
            }
            taskNameEditText = view.findViewById(R.id.taskNameEditText)
            descriptionEditText = view.findViewById(R.id.descriptionEditText)
            durationTextView = view.findViewById(R.id.timerTextView)

            val taskId = firebaseHelper.getTaskId(userId)
            val taskName = taskNameEditText.text.toString()
            val description = descriptionEditText.text.toString()
            val durationText = durationTextView.text.toString()
            val splitDuration = durationText.split(":")
            val hours = splitDuration[0].toInt()
            val minutes = splitDuration[1].toInt()
            val seconds = splitDuration[2].toInt()
            val duration: Int = (hours * 3600) + (minutes * 60) + seconds

            val date = taskDate

            Log.d("TaskPhoto", taskPhoto)

            val newTask = Task(
                taskId, taskName,
                description, taskPhoto, taskTeam,
                taskCategory, duration, date, false, 1, 1
            )
            firebaseHelper.addTask(newTask, userId)
            setupSettingsButton()
            taskAdapter.addTask(newTask)
            toggleVisibility()
        }

        // Setup Categories

        var addNewCategoryButton: Button = view.findViewById(R.id.addNewCategory)
        teamList = view.findViewById(R.id.chooseTeamList)
        createCategoryView = view.findViewById(R.id.createNewCategory)

        addNewCategoryButton.setOnClickListener {
            toggleAddCategory()
        }

        view.findViewById<Button>(R.id.buttonSaveNewCategory).setOnClickListener {
            val name = view.findViewById<EditText>(R.id.editTextCategoryName).text.toString().trim()
            val newCategory = Category(null, name)

            if (name.isNotEmpty()) {
                firebaseHelper.addCategoryToFirebase(newCategory, userId)
            } else {
                Toast.makeText(context, "Please enter a category name.", Toast.LENGTH_SHORT).show()
            }
            toggleAddCategory()

        }

        view.findViewById<Button>(R.id.buttonCancelNewCategory).setOnClickListener {
            toggleAddCategory()
        }

        // Retrieve Categories
        autoCompleteTextView = view.findViewById(R.id.autoCompleteTextViewCategory)

        val categories = mutableListOf<String>()
        categoriesAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, categories)
        autoCompleteTextView.setAdapter(categoriesAdapter)

        firebaseHelper.fetchCategories(userId, categories, categoriesAdapter)

        // set autocomplete
        autoCompleteTextView.setOnClickListener {
            autoCompleteTextView.showDropDown()
        }

        autoCompleteTextView.setOnFocusChangeListener { view, hasFocus ->
            if (hasFocus) autoCompleteTextView.showDropDown()
        }

        autoCompleteTextView.setOnItemClickListener { adapterView, view, position, id ->
            val selectedCategory = adapterView.getItemAtPosition(position) as String
            // Handle the selected category
        }

        // Done with Categories

        view.findViewById<Button>(R.id.buttonSaveCategory).setOnClickListener {
            val selectedCategory = autoCompleteTextView.text.toString()
            if (selectedCategory.isNotEmpty()) {
                taskCategory = selectedCategory
            } else {
                Toast.makeText(context, "Please select a category.", Toast.LENGTH_SHORT).show()
            }
            toggleCategory()

        }

        view.findViewById<Button>(R.id.buttonCancelCategory).setOnClickListener {
            toggleCategory()
        }

        // Setup Teams

        // Category Section
        val teamButton: Button = view.findViewById(R.id.teamButton)
        teamList = view.findViewById(R.id.chooseTeamList)

        teamButton.setOnClickListener {
            toggleTeam()
        }

        var addNewTeamButton: Button = view.findViewById(R.id.addNewTeam)
        createTeamView = view.findViewById(R.id.createNewTeam)

        addNewTeamButton.setOnClickListener {
            toggleAddTeam()
        }

        view.findViewById<Button>(R.id.buttonSaveNewTeam).setOnClickListener {
            val name = view.findViewById<EditText>(R.id.editTextTeamName).text.toString().trim()

            val newTeam = Team(null, name)

            if (name.isNotEmpty()) {
                firebaseHelper.addTeamToFirebase(newTeam, userId)
            } else {
                Toast.makeText(context, "Please enter a team name.", Toast.LENGTH_SHORT).show()
            }
            toggleAddTeam()
        }

        view.findViewById<Button>(R.id.buttonCancelNewTeam).setOnClickListener {
            toggleAddTeam()
        }

        view.findViewById<Button>(R.id.buttonCancelTeam).setOnClickListener {
            toggleTeam()
        }

        // Retrieve Categories
        teamAutoCompleteTextView = view.findViewById(R.id.autoCompleteTextViewTeam)

        val teams = mutableListOf<String>()
        teamsAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, teams)
        teamAutoCompleteTextView.setAdapter(teamsAdapter)

        firebaseHelper.fetchTeams(userId, teams, teamsAdapter)

        // set autocomplete
        teamAutoCompleteTextView.setOnClickListener {
            teamAutoCompleteTextView.showDropDown()
        }

        teamAutoCompleteTextView.setOnFocusChangeListener { view, hasFocus ->
            if (hasFocus) teamAutoCompleteTextView.showDropDown()
        }

        teamAutoCompleteTextView.setOnItemClickListener { adapterView, view, position, id ->
            val selectedTeam = adapterView.getItemAtPosition(position) as String
        }

        view.findViewById<Button>(R.id.buttonSaveTeam).setOnClickListener {
            val selectedTeam = teamAutoCompleteTextView.text.toString()
            if (selectedTeam.isNotEmpty()) {
                taskTeam = selectedTeam
            } else {
                Toast.makeText(context, "Please select a team.", Toast.LENGTH_SHORT).show()
            }
            toggleTeam()

        }

        view.findViewById<Button>(R.id.buttonCancelCategory).setOnClickListener {
            toggleCategory()
        }

        // Inflate the layout for this fragment
        return view
    }

    /**
     * Called when a duration is set.
     * @param hours The hours part of the duration.
     * @param minutes The minutes part of the duration.
     */
    override fun onDurationSet(hours: Int, minutes: Int) {
        if (timerRunning) {
            stopTimer()
        }
        // Handle the picked duration
        val currentDurationTV: TextView = view?.findViewById(R.id.timerTextView)
            ?: throw IllegalStateException("View cannot be null")
        currentDurationTV.text = "$hours:$minutes:00"
        Toast.makeText(context, "Duration: $hours Hours, $minutes Minutes", Toast.LENGTH_LONG)
            .show()
    }

    /**
     * Called when the duration picker is cancelled.
     */
    override fun onCancel() {
        // Handle cancellation
    }

    /**
     * Launches the image picker to pick an image from the gallery.
     */
    private fun pickImageFromGallery() {
        imagePickerLauncher.launch("image/*")
    }

    /**
     * Shows a dialog explaining why the permission is needed when the permission is not granted.
     */
    private fun showRationaleDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Permission Needed")
            .setMessage("This permission is needed to pick images from your gallery.")
            .setPositiveButton("OK") { dialog, which ->
                permissionLauncher.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE))
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /**
     * Shows a dialog suggesting the user to go to settings when the permission is denied permanently.
     */
    private fun showSettingsDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Permission Denied")
            .setMessage("Permission was denied permanently. You can change this in app settings.")
            .setPositiveButton("Go to Settings") { dialog, which ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = Uri.fromParts("package", requireActivity().packageName, null)
                intent.data = uri
                startActivity(intent)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /**
     * Processes the picked image file by uploading it to Firebase.
     * @param fileUri The URI of the picked image file.
     */
    private fun processPickedImage(fileUri: Uri) {

        progressBar.visibility = View.VISIBLE
        val fileName = UUID.randomUUID().toString() + ".jpg"
        val refStorage = FirebaseStorage.getInstance().reference.child("task_images/$fileName")

        refStorage.putFile(fileUri)
            .addOnSuccessListener {
                refStorage.downloadUrl.addOnSuccessListener { downloadUrl ->
                    Log.d("Firebase", "Image uploaded: $downloadUrl")
                    taskPhoto = downloadUrl.toString()
                    progressBar.visibility = View.GONE
                }
            }
            .addOnFailureListener { e ->
                Log.e("Firebase", "Failed to upload image to Firebase", e)
            }
    }

    /**
     * Shows a date range picker.
     */
    private fun showDateRangePicker() {
        taskRecyclerView.layoutManager = LinearLayoutManager(context)
        taskAdapter = TaskAdapter(tasks, firebaseHelper)
        taskRecyclerView.adapter = taskAdapter

        val userId = firebaseHelper.getUserId()
        firebaseHelper.fetchTasks(userId, tasks, taskAdapter)
        val dateRangePicker = MaterialDatePicker.Builder.dateRangePicker()
            .setTitleText("Select dates")
            .build()

        dateRangePicker.addOnPositiveButtonClickListener { selection ->
            val startDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(selection.first))
            val endDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(selection.second))
            dateRangeTextView.text = "$startDate - $endDate"
            taskAdapter.filterByDateRange(startDate, endDate)
        }

        dateRangePicker.show(childFragmentManager, dateRangePicker.toString())
    }

    /**
     * Shows a date picker.
     */
    private fun showDatePicker() {
        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Select date")
            .build()

        datePicker.addOnPositiveButtonClickListener { selection ->
            taskDate = Date(selection)
        }

        datePicker.show(childFragmentManager, datePicker.toString())
    }

    /**
     * Starts the timer.
     */
    private fun startTimer() {
        timer = object : CountDownTimer(Long.MAX_VALUE, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timeInMilliseconds += 1000
                updateTimer()
            }

            override fun onFinish() {}
        }.start()
        timerRunning = true
    }

    /**
     * Stops the timer.
     */
    private fun stopTimer() {
        timer?.cancel()
        timerRunning = false
    }

    /**
     * Updates the timer display.
     */
    private fun updateTimer() {
        val hours = timeInMilliseconds / 3600000
        val minutes = (timeInMilliseconds % 3600000) / 60000
        val seconds = (timeInMilliseconds % 60000) / 1000

        val timeString = String.format("%d:%02d:%02d", hours, minutes, seconds)
        timerTextView.text = timeString
    }

    /**
     * Sets up the settings button.
     */
    private fun setupSettingsButton() {
        val model: SharedViewModel by activityViewModels()

        val userId = firebaseHelper.getUserId()
        Log.e("HomePage", "User ID: $userId")
        firebaseHelper.fetchTasks(userId) { tasks ->
            // Clear existing data
            model.data.value?.clear()
            // Add new data
            tasks.forEach { task ->
                val newItem = Notification(task.name, task.duration.toString())
                // Use newItem here
                model.data.value?.add(newItem)
            }
            model.adapter.value?.notifyDataSetChanged()
        }
    }

    /**
     * Toggles the visibility of the task details section.
     */
    private fun toggleVisibility() {
        if (enterTaskDetails.visibility == View.GONE) {

            enterTaskDetails.visibility = View.VISIBLE
            navBar.visibility = View.GONE

        } else {
            enterTaskDetails.visibility = View.GONE
            navBar.visibility = View.VISIBLE
            hideKeyboard()
        }
    }

    /**
     * Toggles the visibility of the category list.
     */
    private fun toggleCategory() {
        if (categoryList.visibility == View.GONE) {

            categoryList.visibility = View.VISIBLE


        } else {
            categoryList.visibility = View.GONE

        }
    }

    /**
     * Toggles the visibility of the add category view.
     */
    private fun toggleAddCategory() {
        if (createCategoryView.visibility == View.GONE) {

            createCategoryView.visibility = View.VISIBLE
            categoryList.visibility = View.GONE

        } else {
            createCategoryView.visibility = View.GONE
            categoryList.visibility = View.VISIBLE
        }
    }

    /**
     * Toggles the visibility of the team list.
     */
    private fun toggleTeam() {
        if (teamList.visibility == View.GONE) {

            teamList.visibility = View.VISIBLE


        } else {
            teamList.visibility = View.GONE

        }
    }

    /**
     * Toggles the visibility of the add team view.
     */
    private fun toggleAddTeam() {
        if (createTeamView.visibility == View.GONE) {

            createTeamView.visibility = View.VISIBLE
            teamList.visibility = View.GONE

        } else {
            createTeamView.visibility = View.GONE
            teamList.visibility = View.VISIBLE
        }
    }

    /**
     * Hides the keyboard.
     */
    private fun Fragment.hideKeyboard() {
        val inputMethodManager = requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val currentFocusedView = requireActivity().currentFocus
        currentFocusedView?.let {
            inputMethodManager.hideSoftInputFromWindow(it.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)
        }
    }

    /**
     * Factory method to create a new instance of this fragment using the provided parameters.
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment TimeSheet.
     */
    companion object {
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            TimeSheet().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }

    /**
     * Called when a task is successfully added.
     * @param user The FirebaseUser who added the task.
     */
    override fun onSuccess(user: FirebaseUser?) {
        Toast.makeText(context, "Task added successfully!", Toast.LENGTH_SHORT).show()
    }

    /**
     * Called when adding a task fails.
     * @param errorMessage The error message.
     */
    override fun onFailure(errorMessage: String) {
        Toast.makeText(context, "Failed to add task: $errorMessage", Toast.LENGTH_LONG).show()
    }
}

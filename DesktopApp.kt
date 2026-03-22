import java.awt.*
import java.io.File
import javax.swing.*

fun main() {
    SwingUtilities.invokeLater {
        val app = DesktopTicketApp()
        app.isVisible = true
    }
}

class DesktopTicketApp : JFrame("Ticket Printer Console") {
    
    // State
    private var adultCount = 1
    private var childCount = 0
    private var baseFare = 5.0
    private var selectedToStop = "Induvalu"

    // UI Elements
    private val adultLabel = JLabel("Adult: $adultCount", SwingConstants.CENTER)
    private val childLabel = JLabel("Child: $childCount", SwingConstants.CENTER)
    private val fareLabel = JLabel("Fare: ₹ 5.00", SwingConstants.CENTER).apply {
        font = Font("Arial", Font.BOLD, 24)
        foreground = Color(0, 172, 193) // Accent blue
    }
    
    // Route Data (Parsed from file)
    private val routeData = mutableMapOf<String, Double>()

    init {
        parseRoutes()
        setupUI()
        updateFare()
    }

    private fun parseRoutes() {
        val file = File("english_routes.txt")
        if (file.exists()) {
            file.forEachLine { line ->
                val parts = line.split(",").map { it.trim() }
                if (parts.isNotEmpty() && parts[0] == "Stop") {
                    val stopName = parts[1]
                    var stopFare = 5.0
                    if (parts.size > 2) {
                        try {
                            stopFare = parts[2].toDouble()
                        } catch (e: NumberFormatException) {
                            // Ignored, use default 5.0
                        }
                    }
                    routeData[stopName] = stopFare
                }
            }
        } else {
            // Fallback Samples
            routeData["Kallahalli"] = 6.0
            routeData["Induvalu"] = 8.0
            routeData["Sundalli"] = 10.0
            routeData["Kalenahalli"] = 12.0
            routeData["Tubinakere"] = 14.0
            routeData["Gananguru"] = 16.0
        }
        
        // Ensure default selected stop exists
        if(routeData.isNotEmpty() && !routeData.containsKey(selectedToStop)) {
             selectedToStop = routeData.keys.first()
        }
    }

    private fun setupUI() {
        defaultCloseOperation = EXIT_ON_CLOSE
        setSize(400, 750)
        setLocationRelativeTo(null)
        
        val mainPanel = JPanel()
        mainPanel.layout = BoxLayout(mainPanel, BoxLayout.Y_AXIS)
        mainPanel.background = Color(26, 26, 26) // Dark Background
        mainPanel.border = BorderFactory.createEmptyBorder(20, 20, 20, 20)
        
        // Set global text color to white
        UIManager.put("Label.foreground", Color.WHITE)

        // 1. Header
        val header = JLabel("Trip 1: Mandya to Srirangapatna", SwingConstants.CENTER)
        header.font = Font("Arial", Font.BOLD, 18)
        header.alignmentX = Component.CENTER_ALIGNMENT
        mainPanel.add(header)
        mainPanel.add(Box.createRigidArea(Dimension(0, 20)))

        // 2. From / To Routing
        mainPanel.add(createRouteRow("FROM", "Mandya"))
        mainPanel.add(Box.createRigidArea(Dimension(0, 10)))
        
        val toRowPanel = JPanel(FlowLayout(FlowLayout.LEFT))
        toRowPanel.background = Color(26, 26, 26)
        val toTitle = JButton("TO").apply {
             background = Color(51, 51, 51)
             foreground = Color.WHITE
             isFocusPainted = false
        }
        val toLabel = JLabel(selectedToStop)
        toLabel.font = Font("Arial", Font.PLAIN, 18)
        toRowPanel.add(toTitle)
        toRowPanel.add(toLabel)
        toRowPanel.alignmentX = Component.CENTER_ALIGNMENT
        mainPanel.add(toRowPanel)

        mainPanel.add(Box.createRigidArea(Dimension(0, 20)))

        // 3. Stop Selection Grid
        val gridPanel = JPanel(GridLayout(0, 3, 10, 10))
        gridPanel.background = Color(26, 26, 26)
        
        routeData.keys.take(9).forEach { stopName ->
            val btn = JButton(stopName)
            btn.background = if (stopName == selectedToStop) Color(0, 172, 193) else Color(51, 51, 51)
            btn.foreground = Color.WHITE
            btn.isFocusPainted = false
            btn.addActionListener {
                selectedToStop = stopName
                toLabel.text = stopName
                
                // Reset button colors
                for (comp in gridPanel.components) {
                    if (comp is JButton) comp.background = Color(51, 51, 51)
                }
                btn.background = Color(0, 172, 193)
                updateFare()
            }
            gridPanel.add(btn)
        }
        mainPanel.add(gridPanel)
        mainPanel.add(Box.createRigidArea(Dimension(0, 30)))
        
        // Spacer
        mainPanel.add(Box.createVerticalGlue())

        // 4. Counters
        adultLabel.foreground = Color.WHITE
        adultLabel.font = Font("Arial", Font.PLAIN, 18)
        mainPanel.add(createCounterRow(adultLabel) { change ->
            if (adultCount + change >= 1) adultCount += change
            adultLabel.text = "Adult: $adultCount"
            updateFare()
        })
        
        childLabel.foreground = Color.WHITE
        childLabel.font = Font("Arial", Font.PLAIN, 18)
        mainPanel.add(createCounterRow(childLabel) { change ->
            if (childCount + change >= 0) childCount += change
            childLabel.text = "Child: $childCount"
            updateFare()
        })
        
        mainPanel.add(Box.createRigidArea(Dimension(0, 30)))

        // 5. Action Buttons
        val actionPanel = JPanel(GridLayout(1, 3, 10, 0))
        actionPanel.background = Color(26, 26, 26)
        actionPanel.preferredSize = Dimension(actionPanel.preferredSize.width, 60)
        
        val passBtn = JButton("PASS").apply { background = Color(51, 51, 51); foreground = Color.WHITE }
        val resetBtn = JButton("RESET").apply { background = Color(51, 51, 51); foreground = Color.WHITE }
        val printBtn = JButton("PRINT").apply {
            background = Color(34, 34, 34)
            foreground = Color.WHITE
            font = Font("Arial", Font.BOLD, 22)
            addActionListener {
                JOptionPane.showMessageDialog(this@DesktopTicketApp, 
                    "Ticket Printed!\n\nRoutes: Mandya to $selectedToStop\nAdults: $adultCount, Children: $childCount\nTotal Fare: ${fareLabel.text}", 
                    "Print Success", JOptionPane.INFORMATION_MESSAGE)
            }
        }
        
        actionPanel.add(passBtn)
        actionPanel.add(printBtn)
        actionPanel.add(resetBtn)
        
        mainPanel.add(actionPanel)
        mainPanel.add(Box.createRigidArea(Dimension(0, 20)))

        // 6. Fare Display
        fareLabel.alignmentX = Component.CENTER_ALIGNMENT
        mainPanel.add(fareLabel)
        
        add(mainPanel)
    }

    private fun createRouteRow(title: String, value: String): JPanel {
        val p = JPanel(FlowLayout(FlowLayout.LEFT))
        p.background = Color(26, 26, 26)
        val btn = JButton(title).apply {
            background = Color(51, 51, 51)
            foreground = Color.WHITE
            isFocusPainted = false
        }
        val lbl = JLabel(value).apply {
            font = Font("Arial", Font.PLAIN, 18)
            foreground = Color.WHITE
        }
        p.add(btn)
        p.add(lbl)
        p.alignmentX = Component.CENTER_ALIGNMENT
        return p
    }

    private fun createCounterRow(label: JLabel, onAction: (Int) -> Unit): JPanel {
        val p = JPanel(GridLayout(1, 3, 10, 0))
        p.background = Color(26, 26, 26)
        val minus = JButton("-").apply { background = Color(51, 51, 51); foreground = Color.WHITE; addActionListener { onAction(-1) } }
        val plus = JButton("+").apply { background = Color(51, 51, 51); foreground = Color.WHITE; addActionListener { onAction(1) } }
        
        p.add(minus)
        p.add(label)
        p.add(plus)
        p.alignmentX = Component.CENTER_ALIGNMENT
        return p
    }

    private fun updateFare() {
        val destFare = routeData[selectedToStop] ?: 5.0
        val childFare = destFare * 0.5
        val total = (adultCount * destFare) + (childCount * childFare)
        fareLabel.text = String.format("Fare: ₹ %.2f", total)
    }
}

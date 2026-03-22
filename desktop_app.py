import tkinter as tk
from tkinter import messagebox
import os

# State
adult_count = 1
child_count = 0
selected_to_stop = "Induvalu"
route_data = {}

# Initialization
def parse_routes():
    global selected_to_stop
    file_path = "e:/App/english_routes.txt"
    if os.path.exists(file_path):
        with open(file_path, "r", encoding="utf-8") as f:
            for line in f:
                parts = [p.strip() for p in line.split(",")]
                if parts and parts[0] == "Stop":
                    stop_name = parts[1]
                    stop_fare = 5.0
                    if len(parts) > 2:
                        try:
                            stop_fare = float(parts[2])
                        except ValueError:
                            pass
                    route_data[stop_name] = stop_fare
    
    if not route_data:
        # Fallback Samples
        route_data.update({
            "Kallahalli": 6.0, "Induvalu": 8.0, "Sundalli": 10.0,
            "Kalenahalli": 12.0, "Tubinakere": 14.0, "Gananguru": 16.0
        })
    
    if selected_to_stop not in route_data and route_data:
        selected_to_stop = list(route_data.keys())[0]

def update_fare():
    dest_fare = route_data.get(selected_to_stop, 5.0)
    child_fare = dest_fare * 0.5
    total = (adult_count * dest_fare) + (child_count * child_fare)
    fare_label.config(text=f"Fare: ₹ {total:.2f}")

def update_adult(change):
    global adult_count
    if adult_count + change >= 1:
        adult_count += change
        adult_label.config(text=f"Adult: {adult_count}")
        update_fare()

def update_child(change):
    global child_count
    if child_count + change >= 0:
        child_count += change
        child_label.config(text=f"Child: {child_count}")
        update_fare()

def select_stop(stop_name, btn):
    global selected_to_stop
    selected_to_stop = stop_name
    to_label.config(text=stop_name)
    
    # Reset button colors
    for current_btn in stop_buttons:
        current_btn.config(bg="#333333")
    btn.config(bg="#00ACC1")
    update_fare()

def print_ticket():
    dest_fare = route_data.get(selected_to_stop, 5.0)
    total = (adult_count * dest_fare) + (child_count * (dest_fare * 0.5))
    messagebox.showinfo("Print Success", 
        f"Ticket Printed!\n\nRoutes: Mandya to {selected_to_stop}\n"
        f"Adults: {adult_count}, Children: {child_count}\n"
        f"Total Fare: ₹ {total:.2f}")

# Setup UI
parse_routes()

root = tk.Tk()
root.title("Ticket Printer Console")
root.geometry("400x750")
root.configure(bg="#1A1A1A")

# Title
tk.Label(root, text="Trip 1: Mandya to Srirangapatna", font=("Arial", 16, "bold"), bg="#1A1A1A", fg="white").pack(pady=20)

# FROM row
from_frame = tk.Frame(root, bg="#1A1A1A")
from_frame.pack(fill="x", padx=20, pady=5)
tk.Button(from_frame, text="FROM", bg="#333333", fg="white", font=("Arial", 12, "bold"), width=8, relief="flat").pack(side="left")
tk.Label(from_frame, text="Mandya", font=("Arial", 14), bg="#1A1A1A", fg="white").pack(side="left", padx=15)

# TO row
to_frame = tk.Frame(root, bg="#1A1A1A")
to_frame.pack(fill="x", padx=20, pady=5)
tk.Button(to_frame, text="TO", bg="#333333", fg="white", font=("Arial", 12, "bold"), width=8, relief="flat").pack(side="left")
to_label = tk.Label(to_frame, text=selected_to_stop, font=("Arial", 14), bg="#1A1A1A", fg="white")
to_label.pack(side="left", padx=15)

# Grid of popular stops
grid_frame = tk.Frame(root, bg="#1A1A1A")
grid_frame.pack(pady=20, padx=20, fill="x")

stop_buttons = []
row = 0
col = 0
for stop_name in list(route_data.keys())[:9]:
    bg_color = "#00ACC1" if stop_name == selected_to_stop else "#333333"
    btn = tk.Button(grid_frame, text=stop_name, bg=bg_color, fg="white", font=("Arial", 10), 
                    width=12, height=2, relief="flat")
    btn.grid(row=row, column=col, padx=5, pady=5)
    btn.config(command=lambda s=stop_name, b=btn: select_stop(s, b))
    stop_buttons.append(btn)
    col += 1
    if col > 2:
        col = 0
        row += 1

# Filler space
tk.Frame(root, bg="#1A1A1A", height=20).pack(fill="x", expand=True)

# Counters
adult_frame = tk.Frame(root, bg="#1A1A1A")
adult_frame.pack(fill="x", padx=40, pady=5)
tk.Button(adult_frame, text="-", bg="#333333", fg="white", font=("Arial", 14), width=3, command=lambda: update_adult(-1), relief="flat").pack(side="left")
adult_label = tk.Label(adult_frame, text=f"Adult: {adult_count}", font=("Arial", 14), bg="#1A1A1A", fg="white", width=12)
adult_label.pack(side="left", fill="x", expand=True)
tk.Button(adult_frame, text="+", bg="#333333", fg="white", font=("Arial", 14), width=3, command=lambda: update_adult(1), relief="flat").pack(side="right")

child_frame = tk.Frame(root, bg="#1A1A1A")
child_frame.pack(fill="x", padx=40, pady=5)
tk.Button(child_frame, text="-", bg="#333333", fg="white", font=("Arial", 14), width=3, command=lambda: update_child(-1), relief="flat").pack(side="left")
child_label = tk.Label(child_frame, text=f"Child: {child_count}", font=("Arial", 14), bg="#1A1A1A", fg="white", width=12)
child_label.pack(side="left", fill="x", expand=True)
tk.Button(child_frame, text="+", bg="#333333", fg="white", font=("Arial", 14), width=3, command=lambda: update_child(1), relief="flat").pack(side="right")

# Actions
action_frame = tk.Frame(root, bg="#1A1A1A")
action_frame.pack(fill="x", padx=20, pady=20)
tk.Button(action_frame, text="PASS", bg="#333333", fg="white", font=("Arial", 12, "bold"), height=2, relief="flat").pack(side="left", fill="x", expand=True, padx=5)
tk.Button(action_frame, text="PRINT", bg="#222222", fg="white", font=("Arial", 16, "bold"), height=2, command=print_ticket, relief="flat").pack(side="left", fill="x", expand=True, padx=5)
tk.Button(action_frame, text="RESET", bg="#333333", fg="white", font=("Arial", 12, "bold"), height=2, relief="flat").pack(side="left", fill="x", expand=True, padx=5)

# Fare
fare_label = tk.Label(root, text="Fare: ₹ 5.00", font=("Arial", 22, "bold"), bg="#1A1A1A", fg="#00ACC1")
fare_label.pack(pady=10)

update_fare()
root.mainloop()

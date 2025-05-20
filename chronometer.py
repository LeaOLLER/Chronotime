import tkinter as tk
from tkinter import ttk
from datetime import datetime, timedelta
import json
import os
import matplotlib.pyplot as plt
from matplotlib.figure import Figure
from matplotlib.backends.backend_tkagg import FigureCanvasTkAgg
from collections import defaultdict
import psutil
import time
import re

class ChromeTabTracker:
    def __init__(self):
        self.current_tab = None
        self.tab_times = defaultdict(float)
        self.last_check = time.time()
        
    def get_active_chrome_tab(self):
        try:
            for proc in psutil.process_iter(['pid', 'name', 'cmdline']):
                if 'chrome' in proc.info['name'].lower():
                    cmdline = proc.info['cmdline']
                    if cmdline and '--type=renderer' in cmdline:
                        # Extraire l'URL de la ligne de commande
                        for arg in cmdline:
                            if arg.startswith('--url='):
                                url = arg[6:]
                                # Nettoyer l'URL pour obtenir un titre plus lisible
                                title = self.clean_url(url)
                                return title
        except (psutil.NoSuchProcess, psutil.AccessDenied):
            pass
        return None
    
    def clean_url(self, url):
        # Supprimer le protocole et www
        url = re.sub(r'^https?://(www\.)?', '', url)
        # Supprimer les paramètres après ?
        url = url.split('?')[0]
        # Supprimer le trailing slash
        url = url.rstrip('/')
        return url
    
    def update(self):
        current_time = time.time()
        elapsed = current_time - self.last_check
        
        current_tab = self.get_active_chrome_tab()
        if current_tab:
            if current_tab != self.current_tab:
                self.current_tab = current_tab
            self.tab_times[current_tab] += elapsed
        
        self.last_check = current_time
        return self.current_tab, self.tab_times

class FloatingChronometer:
    def __init__(self):
        self.root = tk.Tk()
        self.root.title("Chronomètre")
        
        # Chargement des données précédentes
        self.data_file = "chrono_sessions.json"
        self.load_sessions()
        
        # Initialisation du tracker d'onglets Chrome
        self.tab_tracker = ChromeTabTracker()
        
        # Configuration de la fenêtre flottante
        self.root.attributes('-topmost', True)
        self.root.overrideredirect(True)
        self.root.geometry('200x140+1100+10')  # Hauteur augmentée pour le combobox
        
        # Barre de titre personnalisée
        self.title_bar = tk.Frame(self.root, bg='#2c2c2c', height=20)
        self.title_bar.pack(fill=tk.X, side=tk.TOP)
        self.title_bar.bind('<Button-1>', self.start_move)
        self.title_bar.bind('<B1-Motion>', self.on_move)
        
        # Boutons de contrôle de fenêtre
        self.close_button = tk.Button(self.title_bar, text='×', command=self.on_closing, 
                                    bg='#2c2c2c', fg='white', bd=0, font=('Arial', 12),
                                    width=2, highlightthickness=0)
        self.close_button.pack(side=tk.RIGHT)
        
        self.size_button = tk.Button(self.title_bar, text='□', command=self.toggle_size,
                                   bg='#2c2c2c', fg='white', bd=0, font=('Arial', 12),
                                   width=2, highlightthickness=0)
        self.size_button.pack(side=tk.RIGHT)
        
        # Variables
        self.running = False
        self.start_time = None
        self.elapsed_time = timedelta()
        self.is_large = True
        self.current_category = tk.StringVar()
        
        # Définition des couleurs par catégorie
        self.category_colors = {
            "Études": "#4CAF50",    # Vert
            "Thales": "#2196F3",    # Bleu
            "Lecture": "#FF9800",   # Orange
            "Autre": "#9C27B0"      # Violet
        }
        
        # Interface principale
        self.main_frame = tk.Frame(self.root)
        self.main_frame.pack(expand=True, fill=tk.BOTH)
        
        # Menu déroulant pour les catégories
        self.categories = list(self.category_colors.keys())
        self.category_combo = ttk.Combobox(self.main_frame, 
                                         textvariable=self.current_category,
                                         values=self.categories)
        self.category_combo.pack(pady=5)
        self.category_combo.set("Études")  # Valeur par défaut
        self.category_combo.bind('<<ComboboxSelected>>', self.update_colors)
        
        # Affichage du temps
        self.time_label = tk.Label(self.main_frame, text="00:00:00", 
                                 font=('Arial', 30),
                                 fg=self.category_colors["Études"])
        self.time_label.pack(pady=5)
        
        # Boutons de contrôle du chronomètre
        self.button_frame = tk.Frame(self.main_frame)
        self.button_frame.pack()
        
        self.start_button = tk.Button(self.button_frame, text="▶", command=self.toggle_chronometer, font=('Arial', 14))
        self.start_button.pack(side=tk.LEFT, padx=5)
        
        self.reset_button = tk.Button(self.button_frame, text="⟲", command=self.reset_chronometer, font=('Arial', 14))
        self.reset_button.pack(side=tk.LEFT, padx=5)
        
        self.finish_button = tk.Button(self.button_frame, text="✓", command=self.finish_session, font=('Arial', 14))
        self.finish_button.pack(side=tk.LEFT, padx=5)
        
        self.stats_button = tk.Button(self.button_frame, text="📊", command=self.show_stats, font=('Arial', 14))
        self.stats_button.pack(side=tk.LEFT, padx=5)
        
        # Ajout du label pour l'onglet actif
        self.tab_label = tk.Label(self.main_frame, text="Aucun onglet détecté", 
                                font=('Arial', 8), wraplength=180)
        self.tab_label.pack(pady=2)
        
        self.update_time()
        
        # Protocole de fermeture
        self.root.protocol("WM_DELETE_WINDOW", self.on_closing)
        
    def load_sessions(self):
        self.sessions = {}
        if os.path.exists(self.data_file):
            try:
                with open(self.data_file, 'r') as f:
                    self.sessions = json.load(f)
            except:
                self.sessions = {}
                
    def save_session(self):
        if self.elapsed_time.total_seconds() > 0:
            category = self.current_category.get()
            if category not in self.sessions:
                self.sessions[category] = []
            
            session = {
                'date': datetime.now().strftime('%Y-%m-%d %H:%M:%S'),
                'duration': str(self.elapsed_time).split('.')[0],
                'duration_seconds': self.elapsed_time.total_seconds()
            }
            self.sessions[category].append(session)
            
            with open(self.data_file, 'w') as f:
                json.dump(self.sessions, f)
                
    def on_closing(self):
        self.save_session()
        self.root.destroy()
        
    def start_move(self, event):
        self.x = event.x
        self.y = event.y
        
    def on_move(self, event):
        deltax = event.x - self.x
        deltay = event.y - self.y
        x = self.root.winfo_x() + deltax
        y = self.root.winfo_y() + deltay
        self.root.geometry(f"+{x}+{y}")
        
    def toggle_chronometer(self):
        if self.running:
            self.running = False
            self.start_button.config(text="▶")  # Affiche play quand en pause
        else:
            self.running = True
            self.start_button.config(text="⏸")  # Affiche pause quand en marche
            if not self.start_time:
                self.start_time = datetime.now() - self.elapsed_time
    
    def reset_chronometer(self):
        self.running = False
        self.start_time = None
        self.elapsed_time = timedelta()
        self.time_label.config(text="00:00:00")
        self.start_button.config(text="▶")
        
    def update_time(self):
        if self.running and self.start_time:
            self.elapsed_time = datetime.now() - self.start_time
            self.time_label.config(text=str(self.elapsed_time).split('.')[0])
            
            # Mise à jour des statistiques des onglets
            current_tab, tab_times = self.tab_tracker.update()
            if current_tab:
                self.tab_label.config(text=f"Onglet actif: {current_tab}")
        self.root.after(1000, self.update_time)  # Met à jour chaque seconde
        
    def toggle_size(self):
        if self.is_large:
            self.root.geometry('150x100+1100+10')  # Augmenté la hauteur à 100
            self.time_label.configure(font=('Arial', 20))
            self.start_button.configure(font=('Arial', 10))
            self.reset_button.configure(font=('Arial', 10))
        else:
            self.root.geometry('200x120+1100+10')  # Augmenté la hauteur à 120
            self.time_label.configure(font=('Arial', 30))
            self.start_button.configure(font=('Arial', 14))
            self.reset_button.configure(font=('Arial', 14))
        self.is_large = not self.is_large
        
    def run(self):
        self.root.mainloop()

    def update_colors(self, event=None):
        """Met à jour les couleurs en fonction de la catégorie sélectionnée"""
        category = self.current_category.get()
        color = self.category_colors.get(category, "#000000")
        self.time_label.config(fg=color)

    def show_stats(self):
        stats_window = tk.Toplevel(self.root)
        stats_window.title("Statistiques")
        stats_window.geometry('800x600')
        
        # Frame pour le menu de filtrage
        filter_frame = tk.Frame(stats_window)
        filter_frame.pack(fill=tk.X, pady=5, padx=10)
        
        tk.Label(filter_frame, text="Catégorie : ", font=('Arial', 10)).pack(side=tk.LEFT)
        
        # Variable pour stocker la catégorie sélectionnée
        selected_category = tk.StringVar()
        
        # Ajout de l'onglet pour les statistiques des onglets Chrome
        notebook = ttk.Notebook(stats_window)
        notebook.pack(expand=True, fill=tk.BOTH, padx=10, pady=5)
        
        # Onglet des statistiques générales
        general_frame = ttk.Frame(notebook)
        notebook.add(general_frame, text="Statistiques générales")
        
        # Onglet des statistiques des onglets Chrome
        chrome_frame = ttk.Frame(notebook)
        notebook.add(chrome_frame, text="Statistiques Chrome")
        
        # Statistiques des onglets Chrome
        _, tab_times = self.tab_tracker.update()
        if tab_times:
            # Créer un graphique pour les onglets
            fig = Figure(figsize=(10, 4))
            ax = fig.add_subplot(111)
            
            # Trier les onglets par temps
            sorted_tabs = sorted(tab_times.items(), key=lambda x: x[1], reverse=True)
            tabs = [tab[0] for tab in sorted_tabs[:10]]  # Top 10 des onglets
            times = [tab[1]/60 for tab in sorted_tabs[:10]]  # Conversion en minutes
            
            ax.barh(tabs, times, color='#2196F3')
            ax.set_xlabel('Temps (minutes)')
            ax.set_title('Top 10 des onglets les plus visités')
            
            canvas = FigureCanvasTkAgg(fig, master=chrome_frame)
            canvas.draw()
            canvas.get_tk_widget().pack(fill=tk.BOTH, expand=True)
            
            # Ajouter un tableau détaillé
            details_frame = ttk.Frame(chrome_frame)
            details_frame.pack(fill=tk.X, pady=10)
            
            # En-têtes
            ttk.Label(details_frame, text="Onglet", font=('Arial', 10, 'bold')).grid(row=0, column=0, padx=5, pady=5)
            ttk.Label(details_frame, text="Temps (minutes)", font=('Arial', 10, 'bold')).grid(row=0, column=1, padx=5, pady=5)
            
            # Données
            for i, (tab, time_spent) in enumerate(sorted_tabs, 1):
                ttk.Label(details_frame, text=tab).grid(row=i, column=0, padx=5, pady=2)
                ttk.Label(details_frame, text=f"{time_spent/60:.1f}").grid(row=i, column=1, padx=5, pady=2)
        
        # Continuer avec les statistiques générales existantes
        stats_frame = ttk.Frame(general_frame)
        stats_frame.pack(fill=tk.X, pady=5)
        
        graph_frame = ttk.Frame(general_frame)
        graph_frame.pack(fill=tk.BOTH, expand=True, pady=5)
        
        # Créer le menu déroulant
        categories = ["Tout"] + list(self.category_colors.keys())
        category_menu = ttk.Combobox(filter_frame, 
                                   textvariable=selected_category,
                                   values=categories,
                                   state="readonly",
                                   width=15)
        category_menu.pack(side=tk.LEFT, padx=5)
        category_menu.set("Tout")  # Valeur par défaut
        
        # Lier la mise à jour au changement de sélection
        selected_category.trace('w', self.update_graph)
        
        # Afficher les statistiques initiales
        self.update_graph()

    def update_graph(self, *args):
        # Mettre à jour le graphique avec la catégorie sélectionnée
        for widget in stats_frame.winfo_children():
            widget.destroy()
        
        # Recréer les statistiques textuelles
        category = selected_category.get()
        for cat in self.sessions:
            if category == "Tout" or cat == category:
                total_time = sum(session['duration_seconds'] for session in self.sessions[cat])
                hours = total_time // 3600
                minutes = (total_time % 3600) // 60
                
                frame = tk.Frame(stats_frame)
                frame.pack(fill=tk.X, pady=5, padx=10)
                
                color = self.category_colors.get(cat, "#000000")
                label = tk.Label(frame, 
                               text=f"{cat}: {int(hours)}h {int(minutes)}m",
                               font=('Arial', 12),
                               fg=color)
                label.pack(side=tk.LEFT)
                
                details_button = tk.Button(frame, 
                                         text="Détails",
                                         command=lambda c=cat: self.show_session_details(c))
                details_button.pack(side=tk.RIGHT)
        
        # Recréer le graphique
        for widget in graph_frame.winfo_children():
            widget.destroy()
        
        fig = Figure(figsize=(10, 4))
        ax = fig.add_subplot(111)
        
        daily_times = defaultdict(float)
        
        # Calculer le temps total par jour pour la catégorie sélectionnée
        if category == "Tout":
            for cat in self.sessions:
                for session in self.sessions[cat]:
                    date = datetime.strptime(session['date'], '%Y-%m-%d %H:%M:%S').date()
                    daily_times[date] += session['duration_seconds'] / 3600
        else:
            if category in self.sessions:
                for session in self.sessions[category]:
                    date = datetime.strptime(session['date'], '%Y-%m-%d %H:%M:%S').date()
                    daily_times[date] += session['duration_seconds'] / 3600
        
        dates = sorted(daily_times.keys())
        values = [daily_times[date] for date in dates]
        
        # Créer les barres avec la couleur de la catégorie sélectionnée
        color = self.category_colors.get(category, "#2196F3") if category != "Tout" else "#2196F3"
        ax.bar([d.strftime('%d/%m') for d in dates], values, color=color, width=0.2)  # Réduire la largeur des barres
        
        ax.set_title('Temps par jour')
        ax.set_xlabel('Date')
        ax.set_ylabel('Heures')
        plt.setp(ax.xaxis.get_majorticklabels(), rotation=45)
        
        # Ajuster les marges pour centrer les barres
        fig.tight_layout()
        
        canvas = FigureCanvasTkAgg(fig, master=graph_frame)
        canvas.draw()
        canvas.get_tk_widget().pack(fill=tk.BOTH, expand=True, padx=10, pady=5)

    def show_session_details(self, category):
        """Affiche les détails des sessions pour une catégorie"""
        details_window = tk.Toplevel(self.root)
        details_window.title(f"Détails - {category}")
        details_window.geometry('400x300')
        
        # Conteneur avec scrollbar
        container = tk.Frame(details_window)
        container.pack(fill=tk.BOTH, expand=True, padx=10, pady=5)
        
        canvas = tk.Canvas(container)
        scrollbar = ttk.Scrollbar(container, orient="vertical", command=canvas.yview)
        scrollable_frame = tk.Frame(canvas)
        
        scrollable_frame.bind(
            "<Configure>",
            lambda e: canvas.configure(scrollregion=canvas.bbox("all"))
        )
        
        canvas.create_window((0, 0), window=scrollable_frame, anchor="nw")
        canvas.configure(yscrollcommand=scrollbar.set)
        
        # En-tête
        header_frame = tk.Frame(scrollable_frame)
        header_frame.pack(fill=tk.X, pady=5)
        tk.Label(header_frame, text="Date", font=('Arial', 10, 'bold')).pack(side=tk.LEFT, padx=(0,100))
        tk.Label(header_frame, text="Durée", font=('Arial', 10, 'bold')).pack(side=tk.LEFT, padx=(0,50))
        
        # Liste des sessions
        if category in self.sessions:
            for i, session in enumerate(reversed(self.sessions[category])):
                session_frame = tk.Frame(scrollable_frame)
                session_frame.pack(fill=tk.X, pady=2)
                
                tk.Label(session_frame, 
                        text=session['date'],
                        font=('Arial', 10),
                        fg=self.category_colors[category]).pack(side=tk.LEFT)
                
                tk.Label(session_frame, 
                        text=session['duration'],
                        font=('Arial', 10),
                        fg=self.category_colors[category]).pack(side=tk.LEFT, padx=20)
                
                delete_button = tk.Button(session_frame, 
                                        text="Supprimer",  # Texte explicite au lieu d'un emoji
                                        font=('Arial', 8),
                                        cursor="hand2",
                                        command=lambda cat=category, idx=i: self.delete_session(cat, idx))
                delete_button.pack(side=tk.RIGHT)
        
        scrollbar.pack(side=tk.RIGHT, fill=tk.Y)
        canvas.pack(side=tk.LEFT, fill=tk.BOTH, expand=True)

    def delete_session(self, category, index):
        """Supprime une session spécifique"""
        if category in self.sessions:
            # Convertit l'index inversé en index réel
            real_index = len(self.sessions[category]) - 1 - index
            if 0 <= real_index < len(self.sessions[category]):
                del self.sessions[category][real_index]
                # Sauvegarde après suppression
                with open(self.data_file, 'w') as f:
                    json.dump(self.sessions, f)
                # Trouve et détruit la fenêtre de détails
                for widget in self.root.winfo_children():
                    if isinstance(widget, tk.Toplevel) and widget.title().startswith("Détails"):
                        widget.destroy()

    def finish_session(self):
        """Termine la session actuelle et sauvegarde"""
        if self.elapsed_time.total_seconds() > 0:  # Vérifie s'il y a du temps écoulé
            self.running = False
            self.start_button.config(text="▶")
            self.save_session()
            self.reset_chronometer()

if __name__ == "__main__":
    chrono = FloatingChronometer()
    chrono.run() 
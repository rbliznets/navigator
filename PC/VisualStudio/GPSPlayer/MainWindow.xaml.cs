using GMap.NET.MapProviders;
using Microsoft.Win32;
using MySql.Data.MySqlClient;
using NavControlLibrary.Models;
using System;
using System.Collections.Generic;
using System.Windows;

namespace GPSPlayer
{
    /// <summary>
    /// Interaction logic for MainWindow.xaml
    /// </summary>
    public partial class MainWindow : Window
    {
        Model model = new Model();

        public MainWindow()
        {
            InitializeComponent();
        }

        private void OpenFile(object sender, RoutedEventArgs e)
        {
            OpenFileDialog openFileDialog1 = new OpenFileDialog
            {
                Title = "Импортировать файл логов MQTT",
                DefaultExt = "log",
                Filter = "log files (*.log)|*.log"
            };
            if (openFileDialog1.ShowDialog() == true)
            {
                model.Open(openFileDialog1.FileName);
                if (model.FileName != "") Title = model.FileName;

                if (Lines.IsChecked == true) Map.DrawRoute(model.GetRoute());
                else Map.DrawRoute(new List<GPSData>());
                Markers.IsChecked = false;
            }
        }

        private void Window_Loaded(object sender, RoutedEventArgs e)
        {
            DataContext = model;

            Map.Bind(model.mMapModel);

            model.Open(Properties.Settings.Default.FileName);
            if (model.FileName != "") Title = model.FileName;

            Top = Properties.Settings.Default.Top;
            Left = Properties.Settings.Default.Left;
            Width = Properties.Settings.Default.Width;
            Height = Properties.Settings.Default.Height;
            WindowState = (WindowState)WindowState.Parse(typeof(WindowState), Properties.Settings.Default.WindowState);

            Map.AddRoute(Properties.Settings.Default.Route);
            model.mMapModel.Provider = GMapProviders.TryGetProvider(Properties.Settings.Default.MapProvider);

            Lines.IsChecked = Properties.Settings.Default.Lines;
        }

        private void Window_Closed(object sender, EventArgs e)
        {
            this.Focus();

            Properties.Settings.Default.MapProvider = model.mMapModel.Provider.ToString();
            Properties.Settings.Default.Route = model.mMapModel.RouteFileName;

            Properties.Settings.Default.Top = Top;
            Properties.Settings.Default.Left = Left;
            Properties.Settings.Default.Width = Width;
            Properties.Settings.Default.Height = Height;
            Properties.Settings.Default.WindowState = WindowState.ToString();

            Properties.Settings.Default.FileName = model.FileName;

            Properties.Settings.Default.Lines = Lines.IsChecked == true;

            Properties.Settings.Default.Save();
        }

        private void Play(object sender, RoutedEventArgs e)
        {
            model.Play();
        }

        private void Stop(object sender, RoutedEventArgs e)
        {
            model.Stop();
        }

        private void ImportFromBD(object sender, RoutedEventArgs e)
        {
            DBSetting win = new DBSetting();
            win.Server.Text = Properties.Settings.Default.Server;
            win.Login.Text = Properties.Settings.Default.Login;
            win.Password.Text = Properties.Settings.Default.Password;

            win.Tid.Value = Properties.Settings.Default.Tid;
            win.Begin.Value = Properties.Settings.Default.Begin;
            win.End.Value = Properties.Settings.Default.End;

            if (win.ShowDialog() == true)
            {
                Properties.Settings.Default.Server = win.Server.Text;
                Properties.Settings.Default.Login = win.Login.Text;
                Properties.Settings.Default.Password = win.Password.Text;
                if (win.Tid.Value != null) Properties.Settings.Default.Tid = (int)win.Tid.Value;
                else Properties.Settings.Default.Tid = 6080;
                if (win.Begin.Value != null) Properties.Settings.Default.Begin = (DateTime)win.Begin.Value;
                else Properties.Settings.Default.Begin = DateTime.Now;
                if (win.End.Value != null) Properties.Settings.Default.End = (DateTime)win.End.Value;
                else Properties.Settings.Default.End = DateTime.Now;

                try
                {
                    MySql.Data.MySqlClient.MySqlConnection conn = new MySql.Data.MySqlClient.MySqlConnection();
                    conn.ConnectionString = "server=" + Properties.Settings.Default.Server + ";uid=" + Properties.Settings.Default.Login + ";pwd=" + Properties.Settings.Default.Password + ";database=mqtt";
                    conn.Open();

                    string sql = "SELECT * FROM location WHERE tid=" + Properties.Settings.Default.Tid.ToString() +
                        " AND time >= '" + Properties.Settings.Default.Begin.ToString("yyyy-MM-dd HH:mm:ss") +
                        "' AND time <= '" + Properties.Settings.Default.End.ToString("yyyy-MM-dd HH:mm:ss") + "'  ORDER BY 'time'";
                    MySqlCommand cmd = new MySqlCommand(sql, conn);
                    MySqlDataReader rdr = cmd.ExecuteReader();

                    model.Open("tid:" + Properties.Settings.Default.Tid.ToString() + " (" + Properties.Settings.Default.Begin.ToString("yyyy-MM-dd HH:mm:ss") + "-" +
                         Properties.Settings.Default.End.ToString("yyyy-MM-dd HH:mm:ss") + ")", rdr);
                    if (model.FileName != "") Title = model.FileName;

                    rdr.Close();
                    conn.Close();

                    if (Lines.IsChecked == true) Map.DrawRoute(model.GetRoute());
                    else Map.DrawRoute(new List<GPSData>());
                    Markers.IsChecked = false;
                }
                catch (Exception ex)
                {
                    MessageBox.Show(ex.ToString());
                }
            }
        }

        private void Lines_Checked(object sender, RoutedEventArgs e)
        {
            Map.DrawRoute(model.GetRoute());
        }

        private void Lines_Unchecked(object sender, RoutedEventArgs e)
        {
            Map.DrawRoute(new List<GPSData>());
        }

        private void Markers_Checked(object sender, RoutedEventArgs e)
        {
            Map.DraMarkers(model.GetRoute());
        }

        private void Markers_Unchecked(object sender, RoutedEventArgs e)
        {
            Map.DraMarkers(new List<GPSData>());
        }

        private void Button_Click(object sender, RoutedEventArgs e)
        {
            model.ClearRoute();

            if (Lines.IsChecked == true) Map.DrawRoute(model.GetRoute());
            else Map.DrawRoute(new List<GPSData>());
            Markers.IsChecked = false;
        }
    }
}

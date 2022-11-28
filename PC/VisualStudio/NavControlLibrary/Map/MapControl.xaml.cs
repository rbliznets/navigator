using GMap.NET;
using GMap.NET.MapProviders;
using GMap.NET.WindowsPresentation;
using Microsoft.Win32;
using NavControlLibrary.Models;
using System.Collections.Generic;
using System.IO;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Media;

namespace NavControlLibrary.Map
{
    /// <summary>
    /// Interaction logic for MapControl.xaml
    /// </summary>
    public partial class MapControl : UserControl, IBindModel
    {
        MapModel mModel = null;
        GMapMarker mBus = new GMapMarker(new PointLatLng());
        ArrowBus mArrowBus = new ArrowBus();

        RouteModel mRoute = new RouteModel();

        public MapControl()
        {
            InitializeComponent();

            MainMap.Position = new PointLatLng(55.4799, 37.3193);
            MainMap.MinZoom = 5;
            MainMap.MaxZoom = 22;
            MainMap.Zoom = 17;
            MainMap.ScaleMode = ScaleModes.Integer;

            ComboBoxMapType.ItemsSource = GMapProviders.List;
            ComboBoxMapType.DisplayMemberPath = "Name";

            MainMap.ShowCenter = false;
            MainMap.CanDragMap = false;

            mBus.Shape = mArrowBus;
            mBus.Offset = new Point(-15, -15);
            MainMap.Markers.Add(mBus);
        }

        public bool Bind(NotifyModel model)
        {
            if (model is MapModel)
            {
                mModel = model as MapModel;
                mModel.PropertyChanged += mModel_PropertyChanged;
                DataContext = mModel;
                mArrowBus.DataContext = mModel;
                return true;
            }
            else return false;
        }

        private void mModel_PropertyChanged(object sender, System.ComponentModel.PropertyChangedEventArgs e)
        {
            if (e.PropertyName == "Point")
            {
                mBus.Position = new(mModel.Point.Lat, mModel.Point.Lng);
            }
        }

        private void Import(object sender, RoutedEventArgs e)
        {
            OpenFileDialog openFileDialog1 = new OpenFileDialog
            {
                Title = "Импортировать файл маршрута",
                DefaultExt = "jsonc",
                Filter = "jsonc files (*.jsonc)|*.jsonc"
            };
            if (openFileDialog1.ShowDialog() == true)
            {
                try
                {
                    var str = mRoute.Import(openFileDialog1.FileName);
                    mModel.RouteFileName = openFileDialog1.FileName;
                    RefreshMarkers();
                    //Wait.Visibility = Visibility.Visible;
                    //timer2.Start();
                }
                catch
                {

                }
            }
        }
        void RefreshMarkers(bool zoom = false)
        {
            MainMap.Markers.Clear();
            foreach (var itm in mRoute.ForwardStops)
            {
                if (itm.IsGPSTrigger)
                {
                    GPSMarker marker = new GPSMarker(itm.GPSTrigger, Color.FromArgb(50, 0, 0, 255), itm.Name + "(вперёд)");
                    marker.AddToMap(MainMap);
                }
            }
            foreach (var itm in mRoute.BackwardStops)
            {
                if (itm.IsGPSTrigger)
                {
                    GPSMarker marker = new GPSMarker(itm.GPSTrigger, Color.FromArgb(50, 0, 255, 0), itm.Name + "(назад)");
                    marker.AddToMap(MainMap);
                }
            }
            foreach (var itm in mRoute.Scripts)
            {
                if (itm.IsGPSTrigger)
                {
                    GPSMarker marker = new GPSMarker(itm.GPSTrigger, Color.FromArgb(50, 255, 255, 0), itm.Name);
                    marker.AddToMap(MainMap);
                }
            }

            MainMap.Markers.Add(mBus);
            if (lines != null) MainMap.Markers.Add(lines);
        }

        public void AddRoute(string fileName)
        {
            if (File.Exists(fileName))
            {
                try
                {
                    var str = mRoute.Import(fileName);
                    mModel.RouteFileName = fileName;
                    RefreshMarkers();
                    //Wait.Visibility = Visibility.Visible;
                    //timer2.Start();
                }
                catch
                {

                }
            }
        }

        protected GMapRoute lines = null;
        public void DrawRoute(List<GPSData> route)
        {
            if (lines != null) MainMap.Markers.Remove(lines);
            List<PointLatLng> points = new List<PointLatLng>();
            foreach (var itm in route)
            {
                points.Add(new PointLatLng(itm.Latitude, itm.Longitude));
            }

            if (points.Count == 0)
            {
                lines = null;
                return;
            }

            lines = new GMapRoute(points);
            MainMap.Markers.Add(lines);
            lines.Shape.Opacity = 0.5;
            (lines.Shape as System.Windows.Shapes.Path).StrokeThickness = 2;

        }

        protected List<GPSPointMarker> markers = new List<GPSPointMarker>();
        public void DraMarkers(List<GPSData> route)
        {
            foreach (var itm in markers)
            {
                itm.RemoveFromMap();
            }
            markers.Clear();

            foreach (var itm in route)
            {
                GPSPointMarker mark = new GPSPointMarker(itm);
                mark.AddToMap(MainMap);
                markers.Add(mark);
            }
        }

        private void Clear(object sender, RoutedEventArgs e)
        {
            mModel.RouteFileName = "";
            mRoute.Reset();
            RefreshMarkers();
        }
    }
}

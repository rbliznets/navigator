using GMap.NET;
using GMap.NET.WindowsPresentation;
using NavControlLibrary.Models;
using System.Collections.Generic;
using System.Windows;
using System.Windows.Media;

namespace NavControlLibrary.Map
{
    public class GPSMarker
    {
        public delegate void eSelectGPSTrigger(GPSTriggerModel gpstrigger);
        public event eSelectGPSTrigger onSelectGPSTrigger;

        public GPSTriggerModel mModel = null;
        GMapControl mMap = null;
        GMapMarker mRadius = new GMapMarker(new PointLatLng());
        MapMarker mRadiusCircle = new MapMarker();
        GMapMarker mPrior = new GMapMarker(new PointLatLng());
        MapMarker mPriorCircle = new MapMarker();
        GMapMarker mPost = new GMapMarker(new PointLatLng());
        MapMarker mPostCircle = new MapMarker();
        GMapMarker mBearing = new GMapMarker(new PointLatLng());
        MapArrow mBearingArrow = new MapArrow();

        public GPSMarker(GPSTriggerModel model, Color? fill = null, string tip = null)
        {
            mModel = model;

            mPrior.Shape = mPriorCircle;
            mPriorCircle.Circle.StrokeThickness = 0;
            mPriorCircle.Circle.Stroke = new SolidColorBrush(Color.FromRgb(243, 16, 243));

            mRadius.Shape = mRadiusCircle;
            if (fill != null)
            {
                mRadiusCircle.Circle.Fill = new SolidColorBrush((Color)fill);
            }
            if (tip != null) mRadiusCircle.ToolTip = tip;

            mPost.Shape = mPostCircle;
            mPostCircle.Circle.StrokeThickness = 0;
            mPostCircle.Circle.Stroke = new SolidColorBrush(Color.FromRgb(0, 0, 255));

            mBearing.Shape = mBearingArrow;
            mBearing.Offset = new Point(-15, -15);

            mRadius.Position = new(mModel.Latitude, mModel.Longitude);
            mPost.Position = new(mModel.Latitude, mModel.Longitude);
            mPrior.Position = new(mModel.Latitude, mModel.Longitude);
            mBearing.Position = new(mModel.Latitude, mModel.Longitude);
            mModel.PropertyChanged += Model_PropertyChanged;
        }

        private void Model_PropertyChanged(object sender, System.ComponentModel.PropertyChangedEventArgs e)
        {
            if ((e.PropertyName == "Latitude") || (e.PropertyName == "Longitude"))
            {
                mRadius.Position = new(mModel.Latitude, mModel.Longitude);
                mPost.Position = new(mModel.Latitude, mModel.Longitude);
                mPrior.Position = new(mModel.Latitude, mModel.Longitude);
                mBearing.Position = new(mModel.Latitude, mModel.Longitude);
            }
            if ((e.PropertyName == "Radius") || (e.PropertyName == "Prior") || (e.PropertyName == "Post"))
            {
                if (mMap != null)
                {
                    RedrawRadius();
                    RedrawBearing();
                }
            }
            if ((e.PropertyName == "IsBearing") || (e.PropertyName == "Bearing"))
            {
                if (mMap != null)
                {
                    RedrawBearing();
                }
            }
        }

        public void AddToMap(GMapControl map)
        {
            RemoveFromMap();
            mMap = map;
            mMap.Markers.Add(mBearing);
            mMap.Markers.Add(mPrior);
            mMap.Markers.Add(mRadius);
            mMap.Markers.Add(mPost);
            mRadiusCircle.MouseLeftButtonDown += MRadiusCircle_MouseLeftButtonDown;
            mRadiusCircle.MouseWheel += MRadiusCircle_MouseWheel;
            mMap.OnMapZoomChanged += Map_OnMapZoomChanged;

            RedrawRadius();
            RedrawBearing();
        }

        private void MRadiusCircle_MouseWheel(object sender, System.Windows.Input.MouseWheelEventArgs e)
        {
            if (e.Delta > 0) mMap.Zoom++;
            else mMap.Zoom--;
        }

        private void MRadiusCircle_MouseLeftButtonDown(object sender, System.Windows.Input.MouseButtonEventArgs e)
        {
            onSelectGPSTrigger?.Invoke(mModel);
        }

        public void AddToList(IList<GMapMarker> lst, GMapControl map)
        {
            RemoveFromMap();
            mMap = map;
            lst.Add(mPrior);
            lst.Add(mRadius);
            lst.Add(mPost);
            lst.Add(mBearing);

            mRadiusCircle.MouseLeftButtonDown += MRadiusCircle_MouseLeftButtonDown;
            mRadiusCircle.MouseWheel += MRadiusCircle_MouseWheel;
            mMap.OnMapZoomChanged += Map_OnMapZoomChanged;
        }

        private void Map_OnMapZoomChanged()
        {
            RedrawRadius();
            RedrawBearing();
        }

        public void RemoveFromMap()
        {
            if (mMap != null)
            {
                mMap.OnMapZoomChanged -= Map_OnMapZoomChanged;
                mMap.Markers.Remove(mBearing);
                mMap.Markers.Remove(mPost);
                mMap.Markers.Remove(mRadius);
                mMap.Markers.Remove(mPrior);
                mRadiusCircle.MouseLeftButtonDown -= MRadiusCircle_MouseLeftButtonDown;
                mRadiusCircle.MouseWheel -= MRadiusCircle_MouseWheel;
                mMap = null;
            }
        }
        private void RedrawBearing()
        {
            if (mModel.IsBearing)
            {
                double scale = mMap.MapProvider.Projection.GetGroundResolution((int)mMap.Zoom, mMap.Position.Lat);
                //Debug.WriteLine(scale.ToString());
                double radius = mModel.Radius / scale;

                mBearingArrow.Bearing.Angle = (double)mModel.Bearing;
                mBearingArrow.Scale.ScaleX = (radius / 15);
                mBearingArrow.Scale.ScaleY = (radius / 15);

                mBearingArrow.Visibility = Visibility.Visible;
            }
            else
            {
                mBearingArrow.Visibility = Visibility.Hidden;
            }
        }

        private void RedrawRadius()
        {
            double scale = mMap.MapProvider.Projection.GetGroundResolution((int)mMap.Zoom, mMap.Position.Lat);
            if (scale <= 0.0) return;
            //Debug.WriteLine(scale.ToString());
            double radius = mModel.Radius / scale;
            mRadius.Offset = new Point(-radius, -radius);
            mRadiusCircle.Width = 2 * radius;
            mRadiusCircle.Height = 2 * radius;

            if (mModel.Prior == mModel.Radius)
            {
                mPriorCircle.Circle.StrokeThickness = 0;
            }
            else
            {
                radius = mModel.Prior / scale;
                mPriorCircle.Circle.StrokeThickness = 1;
                mPrior.Offset = new Point(-radius, -radius);
                mPriorCircle.Width = 2 * radius;
                mPriorCircle.Height = 2 * radius;
            }

            if (mModel.Post == mModel.Radius)
            {
                mPostCircle.Circle.StrokeThickness = 0;
            }
            else
            {
                radius = mModel.Post / scale;
                mPostCircle.Circle.StrokeThickness = 1;
                mPost.Offset = new Point(-radius, -radius);
                mPostCircle.Width = 2 * radius;
                mPostCircle.Height = 2 * radius;
            }
        }
    }
}

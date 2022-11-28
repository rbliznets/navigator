using GMap.NET.MapProviders;
using Newtonsoft.Json;
using Newtonsoft.Json.Linq;
using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.IO;
using System.Linq;
using System.Text;

namespace NavControlLibrary.Models
{
    public class RouteModel : NotifyModel, IModelChanged
    {
        public event IModelChanged.eModelChanged onModelChanged;

        public delegate void eGPSTrigger(GPSTriggerModel gpstrigger, ScriptModel script);
        public event eGPSTrigger onAddGPSTrigger;
        public event eGPSTrigger onRemoveGPSTrigger;

        public class ResentFileData : NotifyModel
        {
            public string fileName { get; set; }
            public string routeName { get; set; }
            public DateTime lastDate { get; set; }

            public ResentFileData()
            {

            }
            public ResentFileData(string fn, string rn)
            {
                fileName = fn;
                routeName = rn;
                lastDate = DateTime.Now;
            }

            bool mIsSelected = false;
            [JsonIgnore]
            public bool IsSelected
            {
                get => mIsSelected;
                set
                {
                    mIsSelected = value;
                    NotifyPropertyChanged(nameof(IsSelected));
                }
            }

            public override string ToString()
            {
                return fileName + "(" + routeName + ":" + lastDate.ToString() + ")";
            }
        }

        #region Поля
        ROUTE_TYPES mRouteType = ROUTE_TYPES.BIDIR;
        string mRouteName = "Маршрут";
        ScriptModel mForwardStopEdit = null;
        ScriptModel mBackwardStopEdit = null;
        ScriptModel mScriptEdit = null;

        GMapProvider mMapProvider = GMapProviders.YandexMap;
        bool mChanged = false;
        List<GMapProvider> mProvidersList = new List<GMapProvider>();
        string mLastFile = "";

        bool mIsNew = false;
        #endregion Поля

        #region Свойства
        [JsonIgnore]
        public bool IsNew
        {
            get => mIsNew;
            set
            {
                if (value != mIsNew)
                {
                    mIsNew = value;
                    NotifyPropertyChanged(nameof(IsNew));
                }
            }
        }
        [JsonIgnore]
        public bool IsChanged
        {
            get => mChanged;
            set
            {
                if (value != mChanged)
                {
                    mChanged = value;
                    NotifyPropertyChanged(nameof(IsChanged));
                    NotifyPropertyChanged(nameof(IsDefImport));
                    if (value) onModelChanged?.Invoke(this);
                }
            }
        }
        public string LastFile
        {
            get => mLastFile;
            set
            {
                mLastFile = value;
                NotifyPropertyChanged(nameof(LastFile));
                NotifyPropertyChanged(nameof(IsDefImport));
                foreach (var itm in RecentFiles)
                {
                    itm.IsSelected = (itm.fileName == value);
                }
            }
        }
        [JsonIgnore]
        public bool IsDefImport
        {
            get => mChanged && (LastFile != "");
        }
        [JsonIgnore]
        public BindingList<ResentFileData> RecentFiles { get; }

        [JsonIgnore]
        public List<GMapProvider> ProvidersList
        {
            get
            {
                return mProvidersList;
                //return GMapProviders.List;
            }
        }

        [JsonIgnore]
        public GMapProvider MapProvider
        {
            get
            {
                return mMapProvider;
            }
            set
            {
                mMapProvider = value;
                NotifyPropertyChanged(nameof(MapProvider));
            }
        }
        public string RouteName
        {
            get
            {
                return mRouteName;
            }
            set
            {
                mRouteName = value;
                NotifyPropertyChanged(nameof(RouteName));
                IsChanged = true;
            }
        }
        public ROUTE_TYPES RouteType
        {
            get
            {
                return mRouteType;
            }
            set
            {
                mRouteType = value;
                NotifyPropertyChanged("RouteType");
                IsChanged = true;
            }
        }
        [JsonIgnore]
        public List<ROUTE_TYPES> RouteTypeList
        {
            get
            {
                return Enum.GetValues<ROUTE_TYPES>().ToList();
            }
        }
        public BindingList<ScriptModel> ForwardStops { get; }
        public ScriptModel ForwardStopEdit
        {
            get { return mForwardStopEdit; }
            set
            {
                mForwardStopEdit = value;
                NotifyPropertyChanged("ForwardStopEdit");
            }
        }
        public BindingList<ScriptModel> BackwardStops { get; }
        public ScriptModel BackwardStopEdit
        {
            get { return mBackwardStopEdit; }
            set
            {
                mBackwardStopEdit = value;
                NotifyPropertyChanged(nameof(BackwardStopEdit));
            }
        }
        public BindingList<ScriptModel> Scripts { get; }
        public ScriptModel ScriptEdit
        {
            get => mScriptEdit;
            set
            {
                mScriptEdit = value;
                NotifyPropertyChanged(nameof(ScriptEdit));
            }
        }
        #endregion Свойства

        public RouteModel()
        {
            RecentFiles = new BindingList<ResentFileData>();
            ForwardStops = new BindingList<ScriptModel>();
            BackwardStops = new BindingList<ScriptModel>();
            Scripts = new BindingList<ScriptModel>();
            ForwardStops.ListChanged += Scripts_ListChanged;
            BackwardStops.ListChanged += Scripts_ListChanged;
            Scripts.ListChanged += Scripts_ListChanged;

            mProvidersList.Add(GMapProviders.YandexMap);
            mProvidersList.Add(GMapProviders.YandexSatelliteMap);
            mProvidersList.Add(GMapProviders.YandexHybridMap);
            mProvidersList.Add(GMapProviders.GoogleMap);
            mProvidersList.Add(GMapProviders.GoogleSatelliteMap);
            mProvidersList.Add(GMapProviders.GoogleHybridMap);
            mProvidersList.Add(GMapProviders.OpenStreetMap);
        }

        private void Scripts_ListChanged(object sender, ListChangedEventArgs e)
        {
            if (e.ListChangedType == ListChangedType.ItemAdded)
            {
                ScriptModel md = ((BindingList<ScriptModel>)sender)[e.NewIndex];
                md.onChangeDirTriggerOn += ChangeDirOn;
                md.onModelChanged += Md_onModelChanged;
                md.onAddGPSTrigger += Md_onAddGPSTrigger;
                md.onRemoveGPSTrigger += Md_onRemoveGPSTrigger;
            }
            if (e.ListChangedType != ListChangedType.ItemChanged)
                IsChanged = true;
        }

        private void Md_onRemoveGPSTrigger(GPSTriggerModel gpstrigger, ScriptModel script)
        {
            onRemoveGPSTrigger?.Invoke(gpstrigger, script);
        }

        private void Md_onAddGPSTrigger(GPSTriggerModel gpstrigger, ScriptModel script)
        {
            onAddGPSTrigger?.Invoke(gpstrigger, script);
        }

        private void Md_onModelChanged(IModelChanged sender)
        {
            IsChanged = true;
        }

        private void ChangeDirOn(ScriptModel script)
        {
            foreach (var s in ForwardStops)
            {
                if (s != script)
                {
                    if (s.ChangedirTrigger) s.ChangedirTrigger = false;
                }
            }
            foreach (var s in BackwardStops)
            {
                if (s != script)
                {
                    if (s.ChangedirTrigger) s.ChangedirTrigger = false;
                }
            }
            foreach (var s in Scripts)
            {
                if (s != script)
                {
                    if (s.ChangedirTrigger) s.ChangedirTrigger = false;
                }
            }
        }

        public bool Export(string fileName)
        {
            string path = fileName.Substring(0, fileName.LastIndexOf("\\") + 1);

            JObject route = new JObject();
            route["version"] = 1;
            route["name"] = RouteName;
            if (RouteType == ROUTE_TYPES.BIDIR) route["type"] = "bidir";
            else route["type"] = "circle";
            var seq = new JArray();
            route["sequences"] = seq;

            int id = 1;
            List<string> files = new List<string>();
            foreach (var itm in ForwardStops)
            {
                seq.Add(itm.GetJToken(id));
                files.AddRange(itm.GetFileList());
                id++;
            }
            foreach (var itm in BackwardStops)
            {
                seq.Add(itm.GetJToken(id));
                files.AddRange(itm.GetFileList());
                id++;
            }
            foreach (var itm in Scripts)
            {
                seq.Add(itm.GetJToken(id));
                files.AddRange(itm.GetFileList());
                id++;
            }

            using (StreamWriter file = new StreamWriter(fileName))
            {
                file.Write(route.ToString());
            }

            foreach (string fl in files.Distinct())
            {
                string dst = path + fl.Substring(fl.LastIndexOf("\\") + 1);
                if (dst != fl)
                {
                    if (File.Exists(dst)) File.Delete(dst);
                    File.Copy(fl, dst);
                }
            }
            RecentFilesAdd(fileName);
            IsChanged = false;
            return true;
        }

        public void RecentFilesAdd(string fileName)
        {
            ResentFileData dt = new ResentFileData(fileName, RouteName);
            var itm = RecentFiles.FirstOrDefault(data => data.fileName == fileName);
            if (itm != null) RecentFiles.Remove(itm);
            RecentFiles.Insert(0, dt);
            while (RecentFiles.Count > 10) RecentFiles.Remove(RecentFiles.Last());
        }

        public string Import(string fileName)
        {
            try
            {
                using (StreamReader file = new StreamReader(fileName))
                {
                    string ln;
                    StringBuilder sb = new StringBuilder();

                    while ((ln = file.ReadLine()) != null)
                    {
                        var index = ln.LastIndexOf("//");
                        if (index >= 0) ln = ln.Substring(0, index);
                        sb.Append(ln.Trim());
                    }
                    file.Close();
                    JObject route = JObject.Parse(sb.ToString());
                    string path = fileName.Substring(0, fileName.LastIndexOf("\\") + 1);
                    string res = Load(route, path);
                    RecentFilesAdd(fileName);
                    IsChanged = false;
                    return res;
                }
            }
            catch (Exception e)
            {
                return "Не удалось прочитать " + fileName + "(" + e.ToString() + ")";
            }
        }
        public string Load(JObject file, string path)
        {
            if ((int)file["version"] != 1) return "версия != 1";
            RouteName = (string)file["name"];
            if ((string)file["type"] == "bidir") RouteType = ROUTE_TYPES.BIDIR;
            else RouteType = ROUTE_TYPES.CIRCLE;

            var sequences = file["sequences"].Children();
            var s = new List<ScriptModel>();
            var f = new List<ScriptModel>();
            var b = new List<ScriptModel>();
            foreach (var x in sequences)
            {
                ScriptModel script = new ScriptModel(x, path);
                if (script.mDir == null) s.Add(script);
                else if (script.mDir == DIR_TYPES.FORWARD) f.Add(script);
                else if (script.mDir == DIR_TYPES.BACKWARD) b.Add(script);
            }

            Scripts.Clear();
            s.Sort((x, y) => ((int)x.mId) - ((int)y.mId));
            s.ForEach(x => Scripts.Add(x));
            ScriptEdit = Scripts.FirstOrDefault();

            SortStops(ForwardStops, f);
            ForwardStopEdit = ForwardStops.FirstOrDefault();

            SortStops(BackwardStops, b);
            BackwardStopEdit = BackwardStops.FirstOrDefault();

            //foreach (var x in BackwardStops) Debug.WriteLine(x.ToString());

            return null;
        }

        private void SortStops(BindingList<ScriptModel> stops, List<ScriptModel> f)
        {
            stops.Clear();
            var x = f.Find(s => s.mNextStop == 0);
            while (x != null)
            {
                stops.Insert(0, x);
                x = f.Find(s => s.mNextStop == x.mId);
            }
        }

        internal void Save(string fileName)
        {
            var res = JsonConvert.SerializeObject(this);
            StreamWriter wrt = new StreamWriter(fileName);
            wrt.Write(res);
            wrt.Close();
            RecentFilesAdd(fileName);
        }

        internal void Open(string fileName)
        {
            RecentFilesAdd(fileName);
        }

        public void Reset()
        {
            RouteType = ROUTE_TYPES.BIDIR;
            RouteName = "Маршрут";
            ForwardStopEdit = null;
            BackwardStopEdit = null;
            ScriptEdit = null;

            ForwardStops.Clear();
            BackwardStops.Clear();
            Scripts.Clear();
        }
    }
}

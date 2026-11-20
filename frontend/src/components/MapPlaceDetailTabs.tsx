import { ReactNode } from 'react'

export interface MapPlaceDetailTab<T extends string> {
  id: T
  label: string
}

interface MapPlaceDetailTabsProps<T extends string> {
  tabs: MapPlaceDetailTab<T>[]
  activeTab: T
  onChange: (tab: T) => void
  children: ReactNode
}

export function MapPlaceDetailTabs<T extends string>({
  tabs,
  activeTab,
  onChange,
  children,
}: MapPlaceDetailTabsProps<T>) {
  return (
    <>
      <nav className="map-place-detail-tabs" aria-label="장소 상세 메뉴" role="tablist">
        {tabs.map((tab) => (
          <button
            type="button"
            key={tab.id}
            className={activeTab === tab.id ? 'active' : ''}
            role="tab"
            aria-selected={activeTab === tab.id}
            onClick={() => onChange(tab.id)}
          >
            {tab.label}
          </button>
        ))}
      </nav>
      <div className="map-place-detail-content" role="tabpanel">
        {children}
      </div>
    </>
  )
}

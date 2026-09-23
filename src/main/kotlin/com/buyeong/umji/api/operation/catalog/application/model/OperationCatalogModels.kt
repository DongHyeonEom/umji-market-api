package com.buyeong.umji.api.operation.catalog.application.model

import java.util.UUID

data class CategoryCommand(val name:String,val parentId:UUID?,val displayOrder:Int,val displayStatus:String)
data class BrandCommand(val name:String,val displayStatus:String)
data class ImageCommand(val storageKey:String,val altText:String?,val displayOrder:Int)
data class OptionValueCommand(val value:String,val displayOrder:Int)
data class OptionCommand(val name:String,val displayOrder:Int,val values:List<OptionValueCommand>)
data class SkuCommand(val skuCode:String,val name:String,val salePrice:Long,val listPrice:Long?,val salesStatus:String,val optionValueIds:Set<UUID>)
data class ProductCommand(val categoryId:UUID,val brandId:UUID?,val name:String,val description:String?,val displayStatus:String,val salesStatus:String,val displayOrder:Int,val images:List<ImageCommand> = emptyList(),val options:List<OptionCommand> = emptyList(),val skus:List<SkuCommand> = emptyList())
data class ProductStatusCommand(val displayStatus:String,val salesStatus:String)
data class CatalogResource(val id:UUID)
data class CategoryView(val id:UUID,val parentId:UUID?,val name:String,val path:String,val depth:Int,val displayOrder:Int,val displayStatus:String)
data class BrandView(val id:UUID,val name:String,val displayStatus:String)
data class SkuView(val id:UUID,val skuCode:String,val name:String,val salePrice:Long,val listPrice:Long?,val salesStatus:String,val optionValueIds:Set<UUID>)
data class ImageView(val id:UUID,val storageKey:String,val altText:String?,val displayOrder:Int)
data class OptionValueView(val id:UUID,val value:String,val displayOrder:Int)
data class OptionView(val id:UUID,val name:String,val displayOrder:Int,val values:List<OptionValueView>)
data class ProductView(val id:UUID,val categoryId:UUID,val brandId:UUID?,val name:String,val description:String?,val displayStatus:String,val salesStatus:String,val displayOrder:Int,val images:List<ImageView> = emptyList(),val options:List<OptionView> = emptyList(),val skus:List<SkuView> = emptyList())
data class ProductPageView(val items:List<ProductView>,val page:Int,val size:Int,val totalElements:Long,val totalPages:Int)
